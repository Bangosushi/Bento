package com.bento.bot.discord.listener;

import com.bento.bot.application.ApplicationSessionStore;
import com.bento.bot.config.BentoProperties;
import com.bento.bot.discord.DiscordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmMessageListener extends ListenerAdapter {

    private final ApplicationSessionStore sessionStore;
    private final DiscordService          discordService;
    private final BentoProperties         props;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Only process DMs, never from the bot itself
        if (!event.isFromType(ChannelType.PRIVATE) || event.getAuthor().isBot()) return;

        long userId = event.getAuthor().getIdLong();

        sessionStore.get(userId).ifPresent(session -> {
            String answer = event.getMessage().getContentRaw().trim();

            if (answer.isBlank()) {
                discordService.sendDm(userId, "⚠️ Please provide a non-empty answer.");
                return;
            }

            session.addAnswer(answer);
            List<String> questions = props.questions();

            if (session.isComplete(questions.size())) {
                discordService.sendApplicationSummary(userId, questions, session.getAnswers());
            } else {
                int nextIdx = session.getCurrentQuestionIndex();
                discordService.sendQuestion(userId, questions.get(nextIdx), nextIdx + 1, questions.size());
            }
        });
    }
}
