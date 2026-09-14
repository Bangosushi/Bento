package com.bento.bot.discord;

import com.bento.bot.application.ApplicationEntity;
import com.bento.bot.application.ApplicationService;
import com.bento.bot.application.ApplicationStatus;
import com.bento.bot.application.QaEntry;
import com.bento.bot.config.BentoProperties;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class DiscordService {

    // @Lazy breaks the circular dep:
    //   JdaConfig → listeners → DiscordService → @Lazy JDA ← JdaConfig
    private final JDA jda;
    private final BentoProperties props;
    private final ApplicationService applicationService;

    @Autowired
    public DiscordService(@Lazy JDA jda,
                          BentoProperties props,
                          ApplicationService applicationService) {
        this.jda = jda;
        this.props = props;
        this.applicationService = applicationService;
    }

    // ── DM helpers ────────────────────────────────────────────

    public void sendDm(long userId, String message) {
        jda.retrieveUserById(userId)
                .flatMap(u -> u.openPrivateChannel())
                .queue(
                        ch -> ch.sendMessage(message).queue(),
                        e -> log.warn("Could not DM user {}: {}", userId, e.getMessage())
                );
    }

    public void sendQuestion(long userId, String question, int number, int total) {
        String formatted = String.format("**Question %d / %d**%n%n%s", number, total, question);
        sendDm(userId, formatted);
    }

    public void sendApplicationSummary(long userId,
                                       List<String> questions,
                                       List<String> answers) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Application Summary")
                .setDescription("Review your answers below. Hit **Redo** to start over or **Send** to submit.")
                .setColor(Color.ORANGE)
                .setTimestamp(Instant.now());

        for (int i = 0; i < questions.size(); i++) {
            embed.addField("Q" + (i + 1) + ": " + questions.get(i), answers.get(i), false);
        }

        jda.retrieveUserById(userId)
                .flatMap(u -> u.openPrivateChannel())
                .queue(ch -> ch.sendMessageEmbeds(embed.build())
                        .addActionRow(
                                Button.danger("app:redo", "🔄 Redo"),
                                Button.success("app:send", "✅ Send")
                        )
                        .queue(),
                        e -> log.warn("Could not send summary to {}: {}", userId, e.getMessage())
                );
    }

    // ── Review channel ────────────────────────────────────────

    /**
     * Posts the initial application embed with Accept / Reject / Ban dropdown
     * to the configured review channel, then stores the message ID on the entity.
     */
    public void postApplicationEmbed(ApplicationEntity app, List<QaEntry> qaEntries) {
        Guild guild = guild();
        TextChannel channel = reviewChannel(guild);
        if (channel == null) return;

        MessageEmbed embed = buildEmbed(app, qaEntries, null);
        StringSelectMenu menu = buildActionMenu(app.getId());

        channel.sendMessageEmbeds(embed)
                .addComponents(ActionRow.of(menu))
                .queue(
                        msg -> applicationService.updateEmbedMessageId(app.getId(), msg.getIdLong()),
                        e -> log.error("Failed to post application embed: {}", e.getMessage())
                );
    }

    /**
     * Edits the existing embed in the review channel to reflect the new status.
     * Removes the select menu when the status is terminal.
     */
    public void updateApplicationEmbed(long messageId, long applicationId, String moderatorTag) {
        Guild guild = guild();
        TextChannel channel = reviewChannel(guild);
        if (channel == null) return;

        applicationService.findById(applicationId).ifPresent(app -> {
            List<QaEntry> qa = applicationService.parseQaEntries(app.getData());
            MessageEmbed newEmbed = buildEmbed(app, qa, moderatorTag);
            boolean terminal = app.getStatus().isTerminal();

            channel.retrieveMessageById(messageId).queue(
                    msg -> {
                        var edit = msg.editMessageEmbeds(newEmbed);
                        if (terminal) {
                            edit.setComponents(); // strip the dropdown
                        }
                        edit.queue(null, e -> log.error("Failed to edit embed {}: {}", messageId, e.getMessage()));
                    },
                    e -> log.error("Failed to retrieve embed message {}: {}", messageId, e.getMessage())
            );
        });
    }

    // ── Interview thread ──────────────────────────────────────

    public void createInterviewThread(ApplicationEntity app) {
        Guild guild = guild();
        TextChannel channel = reviewChannel(guild);
        if (channel == null) return;

        String threadName = "Interview #" + app.getId() + " — " + app.getIgn();

        // true = private thread (requires Discord "Private Threads" — see guild boost level)
        channel.createThreadChannel(threadName, true)
                .queue(
                        thread -> {
                            thread.addThreadMember((User) UserSnowflake.fromId(app.getDiscordId())).queue();
                            thread.sendMessage(
                                    "Welcome <@" + app.getDiscordId() + ">! 🐱\n\n" +
                                    "The moderation team will be with you shortly for your interview. " +
                                    "Feel free to introduce yourself in the meantime!"
                            ).queue();
                            applicationService.updateThreadId(app.getId(), thread.getIdLong());
                        },
                        e -> log.error("Failed to create interview thread: {}", e.getMessage())
                );
    }

    // ── Role management ───────────────────────────────────────

    public void addRole(long userId, long roleId) {
        Guild guild = guild();
        Role role = guild.getRoleById(roleId);
        if (role == null) { log.error("Role {} not found", roleId); return; }
        guild.retrieveMemberById(userId).queue(
                m -> guild.addRoleToMember(m, role).queue(),
                e -> log.warn("Could not retrieve member {} to add role: {}", userId, e.getMessage())
        );
    }

    /**
     * Atomically swaps {@code removeRoleId} for {@code addRoleId} in one API call.
     */
    public void swapRoles(long userId, long removeRoleId, long addRoleId) {
        Guild guild = guild();
        Role remove = guild.getRoleById(removeRoleId);
        Role add    = guild.getRoleById(addRoleId);
        guild.retrieveMemberById(userId).queue(
                m -> {
                    List<Role> toAdd    = add    != null ? List.of(add)    : List.of();
                    List<Role> toRemove = remove != null ? List.of(remove) : List.of();
                    guild.modifyMemberRoles(m, toAdd, toRemove).queue();
                },
                e -> log.warn("Could not retrieve member {} to swap roles: {}", userId, e.getMessage())
        );
    }

    // ── Guild actions ─────────────────────────────────────────

    public void kickMember(long userId, String reason) {
        guild().kick(UserSnowflake.fromId(userId))
                .reason(reason)
                .queue(null, e -> log.warn("Failed to kick {}: {}", userId, e.getMessage()));
    }

    public void sendWelcomeMessage(String ign, long discordId) {
        Guild guild = guild();
        TextChannel general = guild.getTextChannelById(props.discord().generalChannelId());
        if (general == null) { log.error("General channel not found"); return; }
        general.sendMessage(
                "🎉 Welcome to the server, **" + ign + "** (<@" + discordId + ">)! " +
                "Make sure to read the rules and enjoy your stay! 🐱"
        ).queue();
    }

    // ── Private helpers ───────────────────────────────────────

    private Guild guild() {
        return jda.getGuildById(props.discord().guildId());
    }

    private TextChannel reviewChannel(Guild guild) {
        TextChannel ch = guild.getTextChannelById(props.discord().reviewChannelId());
        if (ch == null) log.error("Review channel {} not found", props.discord().reviewChannelId());
        return ch;
    }

    private MessageEmbed buildEmbed(ApplicationEntity app, List<QaEntry> qa, String reviewedBy) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("📋 Application #" + app.getId())
                .setColor(statusColor(app.getStatus()))
                .addField("Status",  statusEmoji(app.getStatus()) + " " + app.getStatus().name(), true)
                .addField("IGN",     app.getIgn(),                                                  true)
                .addField("Discord", "<@" + app.getDiscordId() + ">",                               true)
                .addBlankField(false);

        for (QaEntry entry : qa) {
            eb.addField(entry.question(), entry.answer(), false);
        }

        if (reviewedBy != null) {
            eb.addField("Reviewed by", reviewedBy, true);
        }

        eb.setFooter("Application ID: " + app.getId())
          .setTimestamp(app.getSubmittedAt());

        return eb.build();
    }

    private StringSelectMenu buildActionMenu(long applicationId) {
        return StringSelectMenu.create("app:action:" + applicationId)
                .setPlaceholder("Select action…")
                .addOption("✅ Accept (→ Interview)", "ACCEPT")
                .addOption("❌ Reject",               "REJECT")
                .addOption("🔨 Ban",                  "BAN")
                .build();
    }

    private Color statusColor(ApplicationStatus status) {
        return switch (status) {
            case PENDING   -> Color.ORANGE;
            case INTERVIEW -> new Color(0x4169E1); // royal blue
            case ACCEPTED  -> new Color(0x00C853); // green
            case REJECTED  -> new Color(0xFF5252); // red
            case BANNED    -> new Color(0x6D1A1A); // dark red
        };
    }

    private String statusEmoji(ApplicationStatus status) {
        return switch (status) {
            case PENDING   -> "🟡";
            case INTERVIEW -> "🔵";
            case ACCEPTED  -> "🟢";
            case REJECTED  -> "🔴";
            case BANNED    -> "⛔";
        };
    }
}
