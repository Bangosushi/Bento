package com.bento.bot.discord.listener;

import com.bento.bot.application.ApplicationEntity;
import com.bento.bot.application.ApplicationService;
import com.bento.bot.application.ApplicationStatus;
import com.bento.bot.config.BentoProperties;
import com.bento.bot.discord.DiscordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationSelectListener extends ListenerAdapter {

    private final ApplicationService applicationService;
    private final DiscordService     discordService;
    private final BentoProperties    props;

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("app:action:")) return;

        long applicationId = Long.parseLong(componentId.split(":")[2]);
        String selected    = event.getValues().get(0);
        String modTag      = event.getUser().getAsTag();

        applicationService.findById(applicationId).ifPresentOrElse(
                app -> handle(event, app, selected, modTag),
                () -> event.reply("❌ Application not found.").setEphemeral(true).queue()
        );
    }

    private void handle(StringSelectInteractionEvent event,
                        ApplicationEntity app,
                        String selected,
                        String modTag) {

        if (app.getStatus() != ApplicationStatus.PENDING) {
            event.reply("⚠️ This application is no longer in PENDING status.").setEphemeral(true).queue();
            return;
        }

        switch (selected) {
            case "ACCEPT" -> {
                ApplicationEntity updated = applicationService.startInterview(app.getId());
                discordService.createInterviewThread(updated);
                discordService.addRole(app.getDiscordId(), props.discord().intervieweeRoleId());
                discordService.updateApplicationEmbed(app.getEmbedMessageId(), app.getId(), modTag);
                event.reply("🔵 Application moved to **INTERVIEW**. Thread created!").setEphemeral(true).queue();
                log.info("Application #{} → INTERVIEW by {}", app.getId(), modTag);
            }
            case "REJECT" -> {
                applicationService.updateStatus(app.getId(), ApplicationStatus.REJECTED, "Rejected by " + modTag);
                discordService.updateApplicationEmbed(app.getEmbedMessageId(), app.getId(), modTag);
                discordService.sendDm(app.getDiscordId(),
                        "Thank you for applying to our server. Unfortunately, your application was not accepted this time. " +
                        "You're welcome to apply again in the future! 🐱");
                event.reply("🔴 Application **rejected**.").setEphemeral(true).queue();
                log.info("Application #{} → REJECTED by {}", app.getId(), modTag);
            }
            case "BAN" -> {
                applicationService.updateStatus(app.getId(), ApplicationStatus.BANNED, "Banned by " + modTag);
                discordService.updateApplicationEmbed(app.getEmbedMessageId(), app.getId(), modTag);
                discordService.sendDm(app.getDiscordId(),
                        "Your application has been rejected and you have been flagged from reapplying.");
                discordService.kickMember(app.getDiscordId(), "Banned during application review by " + modTag);
                event.reply("⛔ Application **banned**.").setEphemeral(true).queue();
                log.info("Application #{} → BANNED by {}", app.getId(), modTag);
            }
            default -> event.reply("❌ Unknown action.").setEphemeral(true).queue();
        }
    }
}
