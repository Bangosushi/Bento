package com.bento.bot.discord.listener;

import com.bento.bot.application.ApplicationEntity;
import com.bento.bot.application.ApplicationService;
import com.bento.bot.application.ApplicationSession;
import com.bento.bot.application.ApplicationSessionStore;
import com.bento.bot.application.QaEntry;
import com.bento.bot.config.BentoProperties;
import com.bento.bot.discord.DiscordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SummaryButtonListener extends ListenerAdapter {

    private final ApplicationSessionStore sessionStore;
    private final ApplicationService      applicationService;
    private final DiscordService          discordService;
    private final BentoProperties         props;

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!"app:redo".equals(id) && !"app:send".equals(id)) return;

        long userId = event.getUser().getIdLong();

        if ("app:redo".equals(id)) {
            sessionStore.remove(userId);
            sessionStore.create(userId);
            event.reply("🔄 Application restarted! Let's begin again.").queue();
            discordService.sendQuestion(userId, props.questions().get(0), 1, props.questions().size());
            return;
        }

        // app:send
        sessionStore.get(userId).ifPresentOrElse(
                session -> handleSend(event, session),
                () -> event.reply("⏰ Session expired — please click Apply again to restart.").queue()
        );
    }

    private void handleSend(ButtonInteractionEvent event, ApplicationSession session) {
        long userId = session.getDiscordUserId();
        try {
            List<QaEntry> qa = buildQaEntries(session);
            ApplicationEntity app = applicationService.save(userId, session.getIgn(), qa);
            sessionStore.remove(userId);

            event.reply("✅ Application submitted! We'll review it soon. 🐱").queue();
            discordService.postApplicationEmbed(app, qa);
            log.info("Application #{} submitted by user {}", app.getId(), userId);
        } catch (Exception e) {
            log.error("Failed to save application for user {}: {}", userId, e.getMessage(), e);
            event.reply("❌ Something went wrong saving your application. Please try again.").queue();
        }
    }

    private List<QaEntry> buildQaEntries(ApplicationSession session) {
        List<String> questions = props.questions();
        List<String> answers   = session.getAnswers();
        List<QaEntry> result   = new ArrayList<>(questions.size());
        for (int i = 0; i < questions.size(); i++) {
            result.add(new QaEntry(questions.get(i), answers.get(i)));
        }
        return result;
    }
}
