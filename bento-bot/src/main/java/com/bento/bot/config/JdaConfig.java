package com.bento.bot.config;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class JdaConfig implements DisposableBean {

    private JDA jda;

    /**
     * All @Component classes that extend ListenerAdapter are auto-collected here.
     * DiscordService uses @Lazy JDA to break the circular dep:
     *   JdaConfig → listeners → DiscordService → @Lazy JDA ← JdaConfig
     */
    @Bean
    public JDA jda(BentoProperties props, List<ListenerAdapter> listeners) throws InterruptedException {
        log.info("Starting JDA with {} listeners", listeners.size());
        jda = JDABuilder.createDefault(props.discord().token())
                .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .addEventListeners(listeners.toArray(new Object[0]))
                .build()
                .awaitReady();
        log.info("JDA ready — connected as {}", jda.getSelfUser().getAsTag());
        return jda;
    }

    @Override
    public void destroy() {
        if (jda != null) {
            log.info("Shutting down JDA...");
            jda.shutdown();
        }
    }
}
