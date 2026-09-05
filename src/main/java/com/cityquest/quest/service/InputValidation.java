package com.cityquest.quest.service;

public final class InputValidation {
    public static final int QUEST_NAME_MAX = 120;
    public static final int QUEST_DESCRIPTION_MAX = 50_000;
    public static final int QUEST_RULES_MAX = 100_000;
    public static final int TEAM_NAME_MAX = 120;
    public static final int RIDDLE_DESCRIPTION_MAX = 20_000;
    public static final int ANSWER_MAX = 500;
    public static final int CORRECT_ANSWER_MAX = 200;
    public static final int CORRECT_ANSWERS_TEXT_MAX = 20_000;
    public static final int CORRECT_ANSWERS_COUNT_MAX = 100;
    public static final int ADMIN_DISPLAY_NAME_MAX = 120;

    private InputValidation() {}

    public static String required(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым");
        }
        String normalized = value.trim();
        ensureMaxLength(normalized, fieldName, maxLength);
        return normalized;
    }

    public static String optional(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        ensureMaxLength(normalized, fieldName, maxLength);
        return normalized;
    }

    public static void ensureMaxLength(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " слишком длинное. Максимум " + maxLength + " символов");
        }
    }
}