package com.cityquest.quest.entity;

public enum SubmissionStatus {
    CORRECT("Правильно"),
    WRONG("Неправильно"),
    PENDING_REVIEW("Ожидает проверки");
    private final String displayName;

    SubmissionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}