package com.bento.bot.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    /**
     * Returns true if the user already has an application in one of the given statuses.
     * Used to block duplicate applications while PENDING or in INTERVIEW.
     */
    boolean existsByDiscordIdAndStatusIn(Long discordId, Collection<ApplicationStatus> statuses);
}
