package com.cityquest.quest.service;

import com.cityquest.quest.entity.*;
import com.cityquest.quest.repository.QuestRepository;
import com.cityquest.quest.repository.RiddleRepository;
import com.cityquest.quest.repository.SubmissionRepository;
import com.cityquest.quest.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestService {
    private final QuestRepository questRepository;
    private final RiddleRepository riddleRepository;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;
    private final FileStorageService fileStorageService;

    public QuestService(
            QuestRepository questRepository,
            RiddleRepository riddleRepository,
            TeamRepository teamRepository,
            SubmissionRepository submissionRepository,
            FileStorageService fileStorageService
    ) {
        this.questRepository = questRepository;
        this.riddleRepository = riddleRepository;
        this.teamRepository = teamRepository;
        this.submissionRepository = submissionRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Quest> findAll() {
        return questRepository.findAll();
    }

    public Quest findById(Long id) {
        return questRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Квест с id " + id + " не найден")
                );
    }

    @Transactional
    public Quest create(
            Quest quest,
            MultipartFile backgroundImage,
            MultipartFile checkpointBoundaryImage
    ) {
        prepareQuestDefaults(quest);
        if (backgroundImage != null && !backgroundImage.isEmpty()) {
            quest.setBackgroundImageUrl(
                    fileStorageService.saveImage(backgroundImage, "backgrounds")
            );
        }
        if (checkpointBoundaryImage != null && !checkpointBoundaryImage.isEmpty()) {
            quest.setCheckpointBoundaryImageUrl(
                    fileStorageService.saveImage(checkpointBoundaryImage, "boundaries")
            );
        }
        return questRepository.save(quest);
    }

    @Transactional
    public void updateQuest(
            Long questId,
            String name,
            String description,
            String rules,
            MultipartFile backgroundImage,
            boolean removeBackground,
            MultipartFile checkpointBoundaryImage,
            boolean removeCheckpointBoundaryImage
    ) {
        Quest quest = findById(questId);
        quest.setName(InputValidation.required(
                name,
                "Название квеста",
                InputValidation.QUEST_NAME_MAX
                )
        );

        quest.setDescription(InputValidation.optional(
                description,
                "Описание квеста",
                InputValidation.QUEST_DESCRIPTION_MAX
                )
        );
        quest.setRules(InputValidation.optional(
                rules,
                "Правила квеста",
                com.cityquest.quest.service.InputValidation.QUEST_RULES_MAX
                )
        );

        if (backgroundImage != null && !backgroundImage.isEmpty()) {
            String oldBackground = quest.getBackgroundImageUrl();
            String newBackground = fileStorageService.saveImage(backgroundImage, "backgrounds");
            quest.setBackgroundImageUrl(newBackground);
            fileStorageService.deleteFileAfterCommit(oldBackground);
        } else if (removeBackground) {
            String oldBackground = quest.getBackgroundImageUrl();
            quest.setBackgroundImageUrl(null);
            fileStorageService.deleteFileAfterCommit(oldBackground);
        }

        if (checkpointBoundaryImage != null && !checkpointBoundaryImage.isEmpty()) {
            String oldImage = quest.getCheckpointBoundaryImageUrl();
            String newImage = fileStorageService.saveImage(checkpointBoundaryImage, "boundaries");
            quest.setCheckpointBoundaryImageUrl(newImage);
            fileStorageService.deleteFileAfterCommit(oldImage);
        } else if (removeCheckpointBoundaryImage) {
            String oldImage = quest.getCheckpointBoundaryImageUrl();
            quest.setCheckpointBoundaryImageUrl(null);
            fileStorageService.deleteFileAfterCommit(oldImage);
        }
    }

    @Transactional
    public void deleteQuest(Long questId) {
        Quest quest = findById(questId);
        List<Submission> submissions = submissionRepository.findAllByQuestId(questId);
        List<Riddle> riddles = riddleRepository.findAllByQuestIdOrderByIdAsc(questId);
        List<Team> teams = teamRepository.findAllByQuestIdOrderByNameAsc(questId);
        List<String> filesToDelete = new ArrayList<>();

        if (quest.getBackgroundImageUrl() != null) {
            filesToDelete.add(quest.getBackgroundImageUrl());
        }
        if (quest.getCheckpointBoundaryImageUrl() != null) {
            filesToDelete.add(quest.getCheckpointBoundaryImageUrl());
        }
        for (Submission submission : submissions) {
            if (submission.getPhotoUrl() != null) {
                filesToDelete.add(submission.getPhotoUrl());
            }
        }
        for (Riddle riddle : riddles) {
            if (riddle.getReferencePhotoUrl() != null) {
                filesToDelete.add(riddle.getReferencePhotoUrl());
            }
        }

        submissionRepository.deleteAll(submissions);
        submissionRepository.flush();

        riddleRepository.deleteAll(riddles);
        riddleRepository.flush();

        teamRepository.deleteAll(teams);
        teamRepository.flush();

        questRepository.delete(quest);
        questRepository.flush();

        fileStorageService.deleteFilesAfterCommit(filesToDelete);

    }

    @Transactional
    public void startQuest(Long questId) {
        Quest quest = findById(questId);
        if (quest.getStatus() != QuestStatus.DRAFT) {
            throw new IllegalStateException("Запустить можно только квест в статусе DRAFT");
        }
        quest.setStartedAt(Instant.now());
        quest.setStatus(QuestStatus.ACTIVE);
    }

    @Transactional
    public void finishQuest(Long questId) {
        Quest quest = findById(questId);
        if (quest.getStatus() != QuestStatus.ACTIVE) {
            throw new IllegalStateException("Завершить можно только активный квест");
        }
        quest.setFinishedAt(Instant.now());
        quest.setStatus(QuestStatus.FINISHED);
    }

    @Transactional
    public void updateWrongAttemptPenalty(Long questId, Integer minutes) {
        if (minutes == null || minutes < 0) {
            throw new IllegalArgumentException("Штраф не может быть отрицательным");
        }
        Quest quest = findById(questId);
        quest.setWrongAttemptPenaltyMinutes(minutes);
    }

    @Transactional
    public void freezeScoreboard(Long questId) {
        Quest quest = findById(questId);
        if (quest.getStatus() != QuestStatus.ACTIVE) {
            throw new IllegalStateException("Заморозить результаты можно только во время квеста");
        }
        if (quest.isScoreboardFrozen()) {
            return;
        }
        quest.setScoreboardFrozenAt(Instant.now());
    }

    @Transactional
    public void unfreezeScoreboard(Long questId) {
        Quest quest = findById(questId);
        quest.setScoreboardFrozenAt(null);
    }

    private void prepareQuestDefaults(
            Quest quest
    ) {
        quest.setName(InputValidation.required(
                quest.getName(),
                "Название квеста",
                InputValidation.QUEST_NAME_MAX));
        quest.setDescription(InputValidation.optional(
                quest.getDescription(),
                "Описание квеста",
                InputValidation.QUEST_DESCRIPTION_MAX)
        );

        quest.setRules(InputValidation.optional(
                quest.getRules(),
                "Правила квеста",
                InputValidation.QUEST_RULES_MAX)
        );

        if (quest.getStatus() == null) {
            quest.setStatus(QuestStatus.DRAFT);
        }
        if (quest.getWrongAttemptPenaltyMinutes() == null) {
            quest.setWrongAttemptPenaltyMinutes(20);
        }
    }

    @Transactional
    public void revealScoreboardCell(
            Long questId,
            Long teamId,
            Long riddleId
    ) {
        Quest quest = findById(questId);
        if (!quest.isScoreboardFrozen()) {
            throw new IllegalStateException("Таблица результатов сейчас не заморожена");
        }
        Team team = teamRepository
                .findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Команда не найдена"));
        if (!team.getQuest().getId().equals(questId)) {
            throw new IllegalArgumentException("Команда относится к другому квесту");
        }

        Riddle riddle = riddleRepository
                .findById(riddleId)
                .orElseThrow(() -> new IllegalArgumentException("Загадка не найдена"));
        if (!riddle.getQuest().getId().equals(questId)) {
            throw new IllegalArgumentException("Загадка относится к другому квесту");
        }
        List<Submission> attempts = submissionRepository
                        .findAllByTeamIdAndRiddleIdOrderBySubmittedAtAsc(
                                teamId,
                                riddleId
                        );
        Instant freezeAt = quest.getScoreboardFrozenAt();
        List<Submission> hiddenAttempts = attempts
                .stream()
                .filter(submission -> submission.getSubmittedAt().isAfter(freezeAt))
                .filter(submission ->
                        submission.getScoreboardRevealedAt() == null
                                || submission
                                .getScoreboardRevealedAt()
                                .isBefore(freezeAt)
                )
                .toList();

        if (hiddenAttempts.isEmpty()) {
            return;
        }
        boolean hasPending = hiddenAttempts
                .stream()
                .anyMatch(submission -> submission.getStatus() == SubmissionStatus.PENDING_REVIEW);
        if (hasPending) {
            throw new IllegalStateException("Сначала проверьте ожидающие фото-ответы в этой клетке");
        }
        Instant revealedAt = Instant.now();
        for (Submission submission : hiddenAttempts) {
            submission.setScoreboardRevealedAt(revealedAt);
        }
        submissionRepository.saveAll(hiddenAttempts);
    }
}
