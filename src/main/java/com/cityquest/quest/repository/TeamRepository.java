package com.cityquest.quest.repository;

import com.cityquest.quest.entity.Team;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findAllByQuestIdOrderByNameAsc(Long questId);

    Optional<Team> findByInviteToken(String inviteToken);
}