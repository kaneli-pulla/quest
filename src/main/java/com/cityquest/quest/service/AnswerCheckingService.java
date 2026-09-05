package com.cityquest.quest.service;

import com.cityquest.quest.entity.Riddle;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AnswerCheckingService {
    public boolean isCorrect(Riddle riddle, String submittedAnswer) {
        if (submittedAnswer == null) {
            return false;
        }
        String normalizedSubmitted = normalize(submittedAnswer);
        return riddle
                .getCorrectAnswers()
                .stream()
                .map(this::normalize)
                .anyMatch(answer -> answer.equals(normalizedSubmitted));
    }


    private String normalize(String answer) {
        return answer
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}