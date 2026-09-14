package com.bento.bot.discord;

import com.bento.bot.config.BentoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandRegistrar {

    private final JDA jda;
    private final BentoProperties props;

    /**
     * Fires after the Spring context (and JDA) are fully up.
     * Guild commands update instantly; global commands take up to an hour.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        Guild guild = jda.getGuildById(props.discord().guildId());
        if (guild == null) {
            log.error("Guild {} not found — cannot register slash commands", props.discord().guildId());
            return;
        }

        guild.updateCommands().addCommands(
                Commands.slash("accept", "Accept a player from interview into the server")
                        .addOption(OptionType.INTEGER, "id", "Application ID", true),

                Commands.slash("reject", "Reject an application (PENDING or INTERVIEW stage)")
                        .addOption(OptionType.INTEGER, "id",     "Application ID",  true)
                        .addOption(OptionType.STRING,  "reason", "Rejection reason", true),

                Commands.slash("ban", "Ban an applicant and remove them from the server")
                        .addOption(OptionType.INTEGER, "id",     "Application ID", true)
                        .addOption(OptionType.STRING,  "reason", "Ban reason",      true),

                Commands.slash("setup", "Post the Apply button to #welcome (run once)")

        ).queue(
                cmds -> log.info("Registered {} guild slash commands", cmds.size()),
                e    -> log.error("Failed to register slash commands: {}", e.getMessage())
        );
    }

    /** Posts the Apply embed to #welcome. Called via /setup slash command from SlashCommandListener. */
    public void postApplyButton() {
        Guild guild = jda.getGuildById(props.discord().guildId());
        if (guild == null) return;
        TextChannel welcome = guild.getTextChannelById(props.discord().welcomeChannelId());
        if (welcome == null) { log.error("Welcome channel not found"); return; }

        welcome.sendMessage(
                "## 🐱 Welcome to the server!\n\n" +
                "Want to join our community? Click the button below to apply.\n" +
                "The application takes place in your DMs — make sure they're open!"
        ).addActionRow(Button.primary("app:apply", "📝 Apply")).queue();
    }
}
