package com.bento.bot.discord.listener;

import com.bento.bot.application.ApplicationEntity;
import com.bento.bot.application.ApplicationService;
import com.bento.bot.application.ApplicationStatus;
import com.bento.bot.config.BentoProperties;
import com.bento.bot.discord.CommandRegistrar;
import com.bento.bot.discord.DiscordService;
import com.bento.bot.minecraft.MinecraftClient;
import com.bento.bot.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlashCommandListener extends ListenerAdapter {

    private final ApplicationService applicationService;
    private final PlayerService      playerService;
    private final DiscordService     discordService;
    private final MinecraftClient    minecraftClient;
    private final CommandRegistrar   commandRegistrar;
    private final BentoProperties    props;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "accept" -> handleAccept(event);
            case "reject" -> handleReject(event);
            case "ban"    -> handleBan(event);
            case "setup"  -> handleSetup(event);
        }
    }

    // ── /accept <id> ──────────────────────────────────────────

    private void handleAccept(SlashCommandInteractionEvent event) {
        long appId  = event.getOption("id").getAsLong();
        String mod  = event.getUser().getAsTag();

        applicationService.findById(appId).ifPresentOrElse(app -> {
            if (app.getStatus() != ApplicationStatus.INTERVIEW) {
                event.reply("❌ Application #" + appId + " is not in INTERVIEW status (current: "
                        + app.getStatus() + ").").setEphemeral(true).queue();
                return;
            }

            // DB
            applicationService.accept(appId);

            // Player record
            playerService.create(app.getDiscordId(), app.getIgn());

            // Discord
            discordService.swapRoles(
                    app.getDiscordId(),
                    props.discord().intervieweeRoleId(),
                    props.discord().newKittenRoleId()
            );
            discordService.sendWelcomeMessage(app.getIgn(), app.getDiscordId());
            discordService.updateApplicationEmbed(app.getEmbedMessageId(), appId, mod);

            // Minecraft — survival: whitelist only; creative: whitelist + op
            minecraftClient.whitelistAdd(app.getIgn(), MinecraftClient.Server.SURVIVAL);
            minecraftClient.whitelistAdd(app.getIgn(), MinecraftClient.Server.CREATIVE);
            minecraftClient.op(app.getIgn(),           MinecraftClient.Server.CREATIVE);

            event.reply("✅ **" + app.getIgn() + "** accepted, whitelisted, and welcomed!").queue();
            log.info("Application #{} ACCEPTED by {} — IGN: {}", appId, mod, app.getIgn());

        }, () -> event.reply("❌ Application #" + appId + " not found.").setEphemeral(true).queue());
    }

    // ── /reject <id> <reason> ─────────────────────────────────

    private void handleReject(SlashCommandInteractionEvent event) {
        long   appId  = event.getOption("id").getAsLong();
        String reason = event.getOption("reason").getAsString();
        String mod    = event.getUser().getAsTag();

        applicationService.findById(appId).ifPresentOrElse(app -> {
            if (app.getStatus() != ApplicationStatus.INTERVIEW) {
                event.reply("❌ Application #" + appId + " is not in INTERVIEW status.").setEphemeral(true).queue();
                return;
            }

            applicationService.updateStatus(appId, ApplicationStatus.REJECTED, reason);
            discordService.updateApplicationEmbed(app.getEmbedMessageId(), appId, mod);
            discordService.sendDm(app.getDiscordId(),
                    "Thank you for your time in the interview. Unfortunately we won't be moving forward with your application.\n\n**Reason:** " + reason);

            event.reply("🔴 Application #" + appId + " rejected.").queue();
            log.info("Application #{} REJECTED (interview) by {} — reason: {}", appId, mod, reason);

        }, () -> event.reply("❌ Application #" + appId + " not found.").setEphemeral(true).queue());
    }

    // ── /ban <id> <reason> ────────────────────────────────────

    private void handleBan(SlashCommandInteractionEvent event) {
        long   appId  = event.getOption("id").getAsLong();
        String reason = event.getOption("reason").getAsString();
        String mod    = event.getUser().getAsTag();

        applicationService.findById(appId).ifPresentOrElse(app -> {
            applicationService.updateStatus(appId, ApplicationStatus.BANNED, reason);
            discordService.updateApplicationEmbed(app.getEmbedMessageId(), appId, mod);
            discordService.sendDm(app.getDiscordId(), "You have been banned. Reason: " + reason);
            discordService.kickMember(app.getDiscordId(), reason);

            event.reply("⛔ Application #" + appId + " banned.").queue();
            log.info("Application #{} BANNED by {} — reason: {}", appId, mod, reason);

        }, () -> event.reply("❌ Application #" + appId + " not found.").setEphemeral(true).queue());
    }

    // ── /setup ────────────────────────────────────────────────

    private void handleSetup(SlashCommandInteractionEvent event) {
        commandRegistrar.postApplyButton();
        event.reply("✅ Apply button posted to the welcome channel!").setEphemeral(true).queue();
    }
}
