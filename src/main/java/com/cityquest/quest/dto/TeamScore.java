package com.cityquest.quest.dto;

import java.util.List;

public class TeamScore {
    private final Long teamId;
    private final String teamName;
    private final int solved;
    private final long penaltyMinutes;
    private final List<RiddleScore> riddleResults;

    public TeamScore(
            Long teamId,
            String teamName,
            int solved,
            long penaltyMinutes,
            List<RiddleScore> riddleResults
    ) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.solved = solved;
        this.penaltyMinutes = penaltyMinutes;
        this.riddleResults = riddleResults;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public int getSolved() {
        return solved;
    }

    public long getPenaltyMinutes() {
        return penaltyMinutes;
    }

    public List<RiddleScore> getRiddleResults() {
        return riddleResults;
    }
}