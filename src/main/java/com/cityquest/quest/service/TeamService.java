package com.cityquest.quest.service;

import com.cityquest.quest.entity.Quest;
import com.cityquest.quest.entity.Team;
import com.cityquest.quest.repository.TeamRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final QuestService questService;

    public TeamService(TeamRepository teamRepository, QuestService questService) {
        this.teamRepository = teamRepository;
        this.questService = questService;
    }


    public List<Team> findByQuestId(Long questId) {
        return teamRepository.findAllByQuestIdOrderByNameAsc(questId);
    }


    public Team findById(Long id) {
        return teamRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Команда не найдена"));
    }


    public Team findByInviteToken(String token) {
        return teamRepository
                .findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Некорректная ссылка приглашения"));
    }


    public Team create(Long questId, String name) {
        String normalizedName = InputValidation.required(name, "Название команды", InputValidation.TEAM_NAME_MAX);
        Quest quest =questService.findById(questId);
        Team team = new Team();
        team.setName(normalizedName);
        team.setQuest(quest);
        team.setInviteToken(UUID.randomUUID().toString());
        return teamRepository.save(team);
    }
}