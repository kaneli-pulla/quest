package com.cityquest.quest.entity;

public enum RiddleType {
    TEXT("Текстовый ответ"),
    PHOTO("Фото"),
    TEXT_AND_PHOTO("Текст + фото");
    private final String displayName;

    RiddleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}