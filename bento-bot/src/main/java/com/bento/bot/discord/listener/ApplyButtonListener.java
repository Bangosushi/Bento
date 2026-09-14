package com.bento.bot.discord.listener;

import com.bento.bot.application.ApplicationService;
import com.bento.bot.application.ApplicationSessionStore;
import com.bento.bot.config.BentoProperties;
import com.bento.bot.discord.DiscordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplyButtonListener extends ListenerAdapter {

    private final ApplicationSessionStore sessionStore;
    private final ApplicationService      applicationService;
    private final DiscordService          discordService;
    private final BentoProperties         props;

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!"app:apply".equals(event.getComponentId())) return;

        long userId = event.getUser().getIdLong();

        if (applicationService.hasActiveApplication(userId)) {
            event.reply("❌ You already have a pending or active application!")
                    .setEphemeral(true).queue();
            return;
        }

        if (sessionStore.has(userId)) {
            event.reply("📬 You already have an application in progress — check your DMs!")
                    .setEphemeral(true).queue();
            return;
        }

        sessionStore.create(userId);
        event.reply("📬 Check your DMs! The application is waiting for you.")
                .setEphemeral(true).queue();

        List<String> questions = props.questions();
        discordService.sendQuestion(userId, questions.get(0), 1, questions.size());
        log.info("Application session started for user {}", userId);
    }
}
