package com.bento.bot.scheduler;

import com.bento.bot.config.BentoProperties;
import com.bento.bot.discord.DiscordService;
import com.bento.bot.minecraft.MinecraftClient;
import com.bento.bot.player.PlayerEntity;
import com.bento.bot.player.PlayerRepository;
import com.bento.bot.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactivityScheduler {

    private final PlayerRepository playerRepository;
    private final PlayerService    playerService;
    private final DiscordService   discordService;
    private final MinecraftClient  minecraftClient;
    private final BentoProperties  props;

    @Scheduled(cron = "${bento.inactivity.cron}")
    public void checkInactivity() {
        int noShowDays  = props.inactivity().noShowDays();
        int inactiveDays = props.inactivity().inactiveDays();

        Instant noShowCutoff  = Instant.now().minus(noShowDays,  ChronoUnit.DAYS);
        Instant inactiveCutoff = Instant.now().minus(inactiveDays, ChronoUnit.DAYS);

        List<PlayerEntity> noShows  = playerRepository.findNoShows(noShowCutoff);
        List<PlayerEntity> inactive = playerRepository.findInactive(inactiveCutoff);

        log.info("Inactivity check — no-shows: {}, inactive: {}", noShows.size(), inactive.size());

        Stream.concat(noShows.stream(), inactive.stream()).forEach(player -> {
            try {
                remove(player, noShows.contains(player) ? "no-show" : "inactivity");
            } catch (Exception e) {
                log.error("Error removing player {} ({}): {}", player.getIgn(), player.getId(), e.getMessage());
            }
        });
    }

    private void remove(PlayerEntity player, String reason) {
        log.info("Removing player {} (discordId={}) — reason: {}", player.getIgn(), player.getDiscordId(), reason);

        // 1. Mark inactive in DB first so a concurrent JOIN event doesn't re-activate
        playerService.deactivate(player.getId());

        // 2. Notify user
        String dmReason = reason.equals("no-show")
                ? "You were whitelisted but never joined the server within " + props.inactivity().noShowDays() + " days."
                : "You haven't been seen on the server in " + props.inactivity().inactiveDays() + " days.";
        discordService.sendDm(player.getDiscordId(),
                "👋 You've been removed from the Bento server due to " + reason + ".\n\n" + dmReason +
                "\n\nFeel free to reapply anytime! 🐱");

        // 3. Kick from Discord
        discordService.kickMember(player.getDiscordId(), "Automatic removal: " + reason);

        // 4. Remove from Minecraft (fire-and-forget; errors are logged inside MinecraftClient)
        minecraftClient.whitelistRemove(player.getIgn(), MinecraftClient.Server.SURVIVAL);
        minecraftClient.whitelistRemove(player.getIgn(), MinecraftClient.Server.CREATIVE);
        minecraftClient.deop(player.getIgn(),            MinecraftClient.Server.CREATIVE);
    }
}
