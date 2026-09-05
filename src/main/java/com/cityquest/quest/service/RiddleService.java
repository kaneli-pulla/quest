package com.cityquest.quest.service;

import com.cityquest.quest.entity.*;
import com.cityquest.quest.repository.RiddleRepository;
import com.cityquest.quest.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RiddleService {
    private final RiddleRepository riddleRepository;
    private final SubmissionRepository submissionRepository;
    private final AnswerCheckingService answerCheckingService;
    private final QuestService questService;
    private final FileStorageService fileStorageService;

    public RiddleService(
            RiddleRepository riddleRepository,
            SubmissionRepository submissionRepository,
            QuestService questService,
            FileStorageService fileStorageService,
            AnswerCheckingService answerCheckingService
    ) {
        this.riddleRepository = riddleRepository;
        this.submissionRepository = submissionRepository;
        this.questService = questService;
        this.fileStorageService = fileStorageService;
        this.answerCheckingService = answerCheckingService;
    }

    public List<Riddle> findByQuestId(Long questId) {
        return riddleRepository.findAllByQuestIdOrderByIdAsc(questId);
    }


    public int getNumberInQuest(Long questId, Long riddleId) {
        List<Riddle> riddles = findByQuestId(questId);
        for (int i = 0; i < riddles.size(); i++) {
            if (riddles.get(i).getId().equals(riddleId)) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException("Загадка не найдена в этом квесте");
    }

    public Map<Long, Integer> getNumberMapForQuest(Long questId) {
        List<Riddle> riddles = findByQuestId(questId);
        Map<Long, Integer> numbers = new LinkedHashMap<>();
        for (int i = 0; i < riddles.size(); i++) {
            numbers.put(riddles.get(i).getId(), i + 1);
        }
        return numbers;
    }

    public Riddle findById(Long id) {
        return riddleRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Загадка не найдена"));
    }

    @Transactional
    public Riddle create(
            Long questId,
            Riddle riddle,
            String correctAnswersText,
            MultipartFile referencePhoto
    ) {
        Quest quest = questService.findById(questId);
        riddle.setQuest(quest);
        validateAndNormalizeRiddle(riddle);
        applyAnswers(riddle, correctAnswersText);
        if (requiresPhoto(riddle.getType())) {
            if (referencePhoto == null || referencePhoto.isEmpty()) {
                throw new IllegalArgumentException("Для фото-загадки нужно добавить эталонную фотографию");
            }
            riddle.setReferencePhotoUrl(fileStorageService.saveImage(referencePhoto, "riddles")
            );
        }
        return riddleRepository.save(riddle);
    }


    @Transactional
    public Riddle update(
            Long questId,
            Long riddleId,
            Riddle formRiddle,
            String correctAnswersText,
            MultipartFile referencePhoto
    ) {
        Riddle riddle = findById(riddleId);
        validateQuest(riddle, questId);

        riddle.setDescription(formRiddle.getDescription());
        riddle.setType(formRiddle.getType());
        validateAndNormalizeRiddle(riddle);
        applyAnswers(riddle, correctAnswersText);

        if (requiresPhoto(riddle.getType())) {
            if (referencePhoto != null && !referencePhoto.isEmpty()) {
                String oldPhoto = riddle.getReferencePhotoUrl();
                String newPhoto = fileStorageService.saveImage(referencePhoto, "riddles");
                riddle.setReferencePhotoUrl(newPhoto);
                fileStorageService.deleteFileAfterCommit(oldPhoto);
            } else if (riddle.getReferencePhotoUrl() == null) {
                throw new IllegalArgumentException("Для фото-загадки нужно добавить эталонную фотографию");
            }
        } else {
            fileStorageService.deleteFileAfterCommit(riddle.getReferencePhotoUrl());
            riddle.setReferencePhotoUrl(null);
        }
        recheckTextSubmissions(riddle);
        return riddle;
    }


    private void recheckTextSubmissions(Riddle riddle) {
        if (riddle.getType() != RiddleType.TEXT && riddle.getType() != RiddleType.TEXT_AND_PHOTO) {
            return;
        }
        List<Submission> submissions = submissionRepository.findAllByRiddleIdOrderBySubmittedAtAsc(riddle.getId());
        Instant now = Instant.now();
        for (Submission submission : submissions) {
            if (submission.getStatus() != SubmissionStatus.WRONG) {
                continue;
            }
            if (submission.getAnswerText() == null || submission.getAnswerText().isBlank()) {
                continue;
            }
            if (!answerCheckingService.isCorrect(riddle, submission.getAnswerText())) {
                continue;
            }
            if (riddle.getType() == RiddleType.TEXT) {
                submission.setStatus(SubmissionStatus.CORRECT);
                submission.setJudgedAt(now);
                continue;
            }
            if (wasAutomaticallyRejectedByText(submission)) {
                submission.setStatus(SubmissionStatus.PENDING_REVIEW);
                submission.setJudgedAt(null);
            }
        }

        submissionRepository.saveAll(submissions);
    }


    private boolean wasAutomaticallyRejectedByText(Submission submission) {
        if (submission.getPhotoUrl() == null) {
            return false;
        }
        if (submission.getJudgedAt() == null) {
            return true;
        }
        return submission.getSubmittedAt() != null && submission.getJudgedAt().equals(submission.getSubmittedAt());
    }

    @Transactional
    public void delete(Long questId, Long riddleId) {
        Riddle riddle = findById(riddleId);
        validateQuest(riddle, questId);
        List<Submission> submissions = submissionRepository.findAllByRiddleId(riddleId);
        List<String> filesToDelete = new ArrayList<>();
        for (Submission submission : submissions) {
            if (submission.getPhotoUrl() != null) {
                filesToDelete.add(submission.getPhotoUrl());
            }
        }
        if (riddle.getReferencePhotoUrl() != null) {
            filesToDelete.add(riddle.getReferencePhotoUrl());
        }
        submissionRepository.deleteAll(submissions);
        submissionRepository.flush();
        riddleRepository.delete(riddle);
        riddleRepository.flush();
        fileStorageService.deleteFilesAfterCommit(filesToDelete);
    }

    private void validateAndNormalizeRiddle(Riddle riddle) {
        if (riddle.getType() == null) {
            throw new IllegalArgumentException("Выберите тип загадки");
        }
        riddle.setDescription(InputValidation.optional(
                riddle.getDescription(),
                "Текст загадки",
                InputValidation.RIDDLE_DESCRIPTION_MAX)
        );
    }

    private void applyAnswers(Riddle riddle, String correctAnswersText) {
        Set<String> answers = parseCorrectAnswers(correctAnswersText);
        if (requiresText(riddle.getType()) && answers.isEmpty()) {
            throw new IllegalArgumentException("Для текстовой загадки нужен хотя бы один правильный ответ");
        }
        if (!requiresText(riddle.getType())) {
            answers.clear();
        }
        riddle.setCorrectAnswers(answers);
    }

    private boolean requiresText(RiddleType type) {
        return type == RiddleType.TEXT || type == RiddleType.TEXT_AND_PHOTO;
    }

    private boolean requiresPhoto(RiddleType type) {
        return type == RiddleType.PHOTO || type == RiddleType.TEXT_AND_PHOTO;
    }

    private void validateQuest(Riddle riddle, Long questId) {
        if (!riddle.getQuest().getId().equals(questId)) {
            throw new IllegalArgumentException("Загадка относится к другому квесту");
        }
    }

    private Set<String> parseCorrectAnswers(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashSet<>();
        }
        InputValidation.ensureMaxLength(
                text,
                "Список правильных ответов",
                InputValidation.CORRECT_ANSWERS_TEXT_MAX
        );

        Set<String> answers = Arrays
                .stream(text.split("\\R"))
                .map(String::trim)
                .filter(answer -> !answer.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (answers.size() > InputValidation.CORRECT_ANSWERS_COUNT_MAX) {
            throw new IllegalArgumentException(
                    "Слишком много вариантов "
                            + "правильного ответа. Максимум "
                            + InputValidation
                            .CORRECT_ANSWERS_COUNT_MAX
            );
        }

        for (String answer : answers) {
            InputValidation.ensureMaxLength(
                    answer,
                    "Вариант правильного ответа",
                    InputValidation.CORRECT_ANSWER_MAX
            );
        }

        return answers;
    }
}