package com.cityquest.quest.service;

import com.cityquest.quest.entity.Quest;
import com.cityquest.quest.entity.QuestStatus;
import com.cityquest.quest.entity.Riddle;
import com.cityquest.quest.entity.RiddleType;
import com.cityquest.quest.entity.Submission;
import com.cityquest.quest.entity.SubmissionStatus;
import com.cityquest.quest.entity.Team;
import com.cityquest.quest.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final TeamService teamService;
    private final RiddleService riddleService;
    private final AnswerCheckingService answerCheckingService;
    private final FileStorageService fileStorageService;


    public SubmissionService(
            SubmissionRepository submissionRepository,
            TeamService teamService,
            RiddleService riddleService,
            AnswerCheckingService answerCheckingService,
            FileStorageService fileStorageService
    ) {
        this.submissionRepository = submissionRepository;
        this.teamService = teamService;
        this.riddleService = riddleService;
        this.answerCheckingService = answerCheckingService;
        this.fileStorageService = fileStorageService;
    }


    public List<Submission> findAttempts(Long teamId, Long riddleId) {
        return submissionRepository.findAllByTeamIdAndRiddleIdOrderBySubmittedAtAsc(teamId, riddleId);
    }



    public boolean isSolved(Long teamId, Long riddleId) {
        return submissionRepository.existsByTeamIdAndRiddleIdAndStatus(teamId, riddleId, SubmissionStatus.CORRECT);
    }

    public SubmissionStatus getCurrentStatus(Long teamId, Long riddleId) {
        List<Submission> attempts = findAttempts(teamId, riddleId);
        if (attempts
                .stream()
                .anyMatch(submission -> submission.getStatus() == SubmissionStatus.CORRECT)) {
            return SubmissionStatus.CORRECT;
        }
        if (attempts
                .stream()
                .anyMatch(submission -> submission.getStatus() == SubmissionStatus.PENDING_REVIEW)) {
            return SubmissionStatus.PENDING_REVIEW;
        }


        if (attempts
                .stream()
                .anyMatch(submission -> submission.getStatus() == SubmissionStatus.WRONG)) {
            return SubmissionStatus.WRONG;
        }
        return null;
    }

    public List<Submission>
    findPhotoSubmissionsForQuest(Long questId) {
        List<Submission> submissions = new ArrayList<>(submissionRepository.findPhotoSubmissionsForQuest(questId));
        submissions.sort(
                Comparator
                        .comparing((Submission submission) -> submission.getStatus() != SubmissionStatus.PENDING_REVIEW)
                        .thenComparing(Submission::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
        );
        return submissions;
    }


    public long countPhotoSubmissionsForQuest(Long questId) {
        return submissionRepository.countPhotoSubmissionsForQuest(questId);
    }

    @Transactional
    public Submission submitTextAnswer(Long teamId, Long riddleId, String answerText) {
        Team team = teamService.findById(teamId);
        Riddle riddle = riddleService.findById(riddleId);
        validateTeamAndRiddle(team, riddle);
        validateQuestIsActive(team.getQuest());
        if (isSolved(teamId, riddleId)) {
            throw new IllegalStateException("Эта загадка уже решена");
        }
        if (riddle.getType() != RiddleType.TEXT) {
            throw new IllegalStateException("Для этой загадки требуется фотография");
        }
        String normalizedAnswer = InputValidation.required(answerText, "Ответ", InputValidation.ANSWER_MAX);
        boolean correct = answerCheckingService.isCorrect(riddle, normalizedAnswer);
        Instant now = Instant.now();
        Submission submission = new Submission();
        submission.setTeam(team);
        submission.setRiddle(riddle);
        submission.setAnswerText(normalizedAnswer);
        submission.setSubmittedAt(now);
        submission.setJudgedAt(now);
        submission.setStatus(correct ? SubmissionStatus.CORRECT : SubmissionStatus.WRONG);
        return submissionRepository.save(submission);
    }

    @Transactional
    public Submission submitPhotoAnswer(Long teamId, Long riddleId, String answerText, MultipartFile photo) {
        Team team = teamService.findById(teamId);
        Riddle riddle = riddleService.findById(riddleId);
        validateTeamAndRiddle(team, riddle);
        validateQuestIsActive(team.getQuest());
        if (isSolved(teamId, riddleId)) {
            throw new IllegalStateException("Эта загадка уже решена");
        }
        if (submissionRepository.existsByTeamIdAndRiddleIdAndStatus(teamId, riddleId, SubmissionStatus.PENDING_REVIEW)) {
            throw new IllegalStateException("Предыдущий фото-ответ ещё ожидает проверки");
        }
        if (riddle.getType() == RiddleType.TEXT) {
            throw new IllegalStateException("Для этой загадки фотография не требуется");
        }
        String normalizedAnswer;
        if (riddle.getType() == RiddleType.TEXT_AND_PHOTO) {
            normalizedAnswer = InputValidation.required(answerText, "Ответ", InputValidation.ANSWER_MAX);
        } else {
            normalizedAnswer = InputValidation.optional(answerText, "Ответ", InputValidation.ANSWER_MAX);
        }
        Instant now = Instant.now();
        Submission submission = new Submission();
        submission.setTeam(team);
        submission.setRiddle(riddle);
        submission.setSubmittedAt(now);
        if (riddle.getType() == RiddleType.TEXT_AND_PHOTO && !answerCheckingService.isCorrect(riddle, normalizedAnswer)) {
            submission.setAnswerText(normalizedAnswer);
            submission.setStatus(SubmissionStatus.WRONG);
            submission.setJudgedAt(now);
            return submissionRepository.save(submission);
        }
        String photoUrl = fileStorageService.saveImage(photo, "submissions");
        submission.setPhotoUrl(photoUrl);
        if (!normalizedAnswer.isBlank()) {
            submission.setAnswerText(normalizedAnswer);
        }
        submission.setStatus(SubmissionStatus.PENDING_REVIEW);
        return submissionRepository.save(submission);
    }


    @Transactional
    public void reviewSubmission(Long questId, Long submissionId, boolean correct) {
        Submission submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Ответ не найден"));
        if (!submission.getRiddle().getQuest().getId().equals(questId)) {
            throw new IllegalArgumentException("Ответ относится к другому квесту");
        }
        if (submission.getPhotoUrl() == null) {
            throw new IllegalArgumentException("Это не фото-ответ");
        }
        if (correct && submission.getRiddle().getType() == RiddleType.TEXT_AND_PHOTO &&
                !answerCheckingService.isCorrect(submission.getRiddle(), submission.getAnswerText())) {
            throw new IllegalStateException("Нельзя принять фото: " + "текстовая часть ответа неверна");
        }
        submission.setStatus(correct ? SubmissionStatus.CORRECT : SubmissionStatus.WRONG);
        submission.setJudgedAt(Instant.now());
    }


    private void validateTeamAndRiddle(Team team, Riddle riddle) {
        if (!team.getQuest().getId().equals(riddle.getQuest().getId())) {
            throw new IllegalArgumentException("Эта загадка не относится к квесту команды");
        }
    }


    private void validateQuestIsActive(Quest quest) {
        if (quest.getStatus() != QuestStatus.ACTIVE) {
            throw new IllegalStateException("Сейчас отправлять ответы нельзя");
        }
    }
}