package com.cityquest.quest.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "quests")
public class Quest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String rules;

    @Enumerated(EnumType.STRING)
    private QuestStatus status = QuestStatus.DRAFT;

    private String name;
    private String backgroundImageUrl;
    private String checkpointBoundaryImageUrl;
    private Integer wrongAttemptPenaltyMinutes = 20;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant scoreboardFrozenAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    public void setBackgroundImageUrl(String backgroundImageUrl) {
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public String getCheckpointBoundaryImageUrl() {
        return checkpointBoundaryImageUrl;
    }

    public void setCheckpointBoundaryImageUrl(String checkpointBoundaryImageUrl) {
        this.checkpointBoundaryImageUrl = checkpointBoundaryImageUrl;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }

    public Integer getWrongAttemptPenaltyMinutes() {
        return wrongAttemptPenaltyMinutes;
    }

    public void setWrongAttemptPenaltyMinutes(Integer wrongAttemptPenaltyMinutes) {
        this.wrongAttemptPenaltyMinutes = wrongAttemptPenaltyMinutes;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getScoreboardFrozenAt() {
        return scoreboardFrozenAt;
    }

    public void setScoreboardFrozenAt(Instant scoreboardFrozenAt) {
        this.scoreboardFrozenAt = scoreboardFrozenAt;
    }

    public boolean isScoreboardFrozen() {
        return scoreboardFrozenAt != null;
    }
}