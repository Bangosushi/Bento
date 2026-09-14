package com.bento.bot;

import com.bento.bot.config.BentoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BentoProperties.class)
public class BentoBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(BentoBotApplication.class, args);
    }
}
