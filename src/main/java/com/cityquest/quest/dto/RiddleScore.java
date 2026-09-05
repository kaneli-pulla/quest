package com.cityquest.quest.dto;

public class RiddleScore {
    private final Long riddleId;
    private final boolean solved;
    private final int wrongAttempts;
    private final int pendingAttempts;
    private final int frozenAttempts;
    private final long penaltyMinutes;

    public RiddleScore(
            Long riddleId,
            boolean solved,
            int wrongAttempts,
            int pendingAttempts,
            int frozenAttempts,
            long penaltyMinutes
    ) {
        this.riddleId = riddleId;
        this.solved = solved;
        this.wrongAttempts = wrongAttempts;
        this.pendingAttempts = pendingAttempts;
        this.frozenAttempts = frozenAttempts;
        this.penaltyMinutes = penaltyMinutes;
    }

    public Long getRiddleId() {
        return riddleId;
    }

    public boolean isSolved() {
        return solved;
    }

    public int getWrongAttempts() {
        return wrongAttempts;
    }

    public int getPendingAttempts() {
        return pendingAttempts;
    }

    public int getFrozenAttempts() {
        return frozenAttempts;
    }

    public long getPenaltyMinutes() {
        return penaltyMinutes;
    }
}