package com.bento.bot.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "bento")
@Validated
public record BentoProperties(
        @NotNull Security security,
        @NotNull Discord discord,
        @NotNull Minecraft minecraft,
        @NotNull Inactivity inactivity,
        @NotEmpty List<String> questions
) {

    public record Security(@NotBlank String apiKey) {}

    public record Discord(
            @NotBlank String token,
            @Positive long guildId,
            @Positive long welcomeChannelId,
            @Positive long reviewChannelId,   // #cat-pplications / where mods review
            @Positive long generalChannelId,
            @Positive long intervieweeRoleId,
            @Positive long newKittenRoleId
    ) {}

    public record Minecraft(
            @NotNull Server survival,
            @NotNull Server creative
    ) {
        public record Server(@NotBlank String url) {}
    }

    public record Inactivity(
            @NotBlank String cron,
            @Positive int noShowDays,
            @Positive int inactiveDays
    ) {}
}
