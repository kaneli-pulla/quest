package com.cityquest.quest.entity;

public enum QuestStatus {
    DRAFT("Черновик"),
    ACTIVE("Идёт"),
    FINISHED("Завершён");
    private final String displayName;

    QuestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}