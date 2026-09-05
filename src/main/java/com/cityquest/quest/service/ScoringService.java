package com.cityquest.quest.service;

import com.cityquest.quest.dto.RiddleScore;
import com.cityquest.quest.dto.TeamScore;
import com.cityquest.quest.entity.*;
import com.cityquest.quest.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ScoringService {
    private final TeamService teamService;
    private final RiddleService riddleService;
    private final SubmissionRepository submissionRepository;

    public ScoringService(
            TeamService teamService,
            RiddleService riddleService,
            SubmissionRepository submissionRepository
    ) {
        this.teamService = teamService;
        this.riddleService = riddleService;
        this.submissionRepository = submissionRepository;
    }

    public List<TeamScore> calculateScores(Quest quest, boolean respectFreeze) {
        List<Team> teams = teamService.findByQuestId(quest.getId());
        List<Riddle> riddles = riddleService.findByQuestId(quest.getId());
        Instant cutoff = respectFreeze ? quest.getScoreboardFrozenAt() : null;
        List<TeamScore> scores = new ArrayList<>();
        for (Team team : teams) {
            scores.add(calculateTeamScore(quest, team, riddles, cutoff));
        }
        scores.sort(
                Comparator.comparingInt(TeamScore::getSolved).reversed()
                        .thenComparingLong(TeamScore::getPenaltyMinutes)
                        .thenComparing(TeamScore::getTeamName)
        );
        return scores;
    }

    private TeamScore calculateTeamScore(
            Quest quest,
            Team team,
            List<Riddle> riddles,
            Instant cutoff
    ) {
        List<Submission> submissions = submissionRepository.findAllByTeamIdOrderBySubmittedAtAsc(team.getId());
        List<RiddleScore> riddleResults = new ArrayList<>();
        int solved = 0;
        long totalPenalty = 0;
        for (Riddle riddle : riddles) {
            RiddleScore result = calculateRiddleScore(quest, riddle, submissions, cutoff);
            riddleResults.add(result);
            if (result.isSolved()) {
                solved++;
                totalPenalty += result.getPenaltyMinutes();
            }
        }
        return new TeamScore(
                team.getId(),
                team.getName(),
                solved,
                totalPenalty,
                riddleResults
        );
    }

    private RiddleScore calculateRiddleScore(
            Quest quest,
            Riddle riddle,
            List<Submission> submissions,
            Instant cutoff
    ) {
        List<Submission> allAttempts = submissions
                .stream()
                .filter(s -> s.getRiddle().getId().equals(riddle.getId()))
                .toList();
        List<Submission> visibleAttempts = allAttempts
                .stream()
                .filter(s -> isVisibleAt(s, cutoff))
                .toList();
        int frozenAttempts =
                cutoff == null
                ? 0
                : (int) allAttempts
                        .stream()
                        .filter(s -> !isVisibleAt(s, cutoff))
                        .count();

        Submission correct = visibleAttempts
                .stream()
                .filter(s -> s.getStatus() == SubmissionStatus.CORRECT)
                .findFirst()
                .orElse(null);
        int pendingAttempts = (int) visibleAttempts
                .stream()
                .filter(s -> s.getStatus() == SubmissionStatus.PENDING_REVIEW)
                .count();
        if (correct == null) {
            int wrongAttempts = (int) visibleAttempts
                    .stream()
                    .filter(s -> s.getStatus() == SubmissionStatus.WRONG)
                    .count();
            return new RiddleScore(
                    riddle.getId(),
                    false,
                    wrongAttempts,
                    pendingAttempts,
                    frozenAttempts,
                    0
            );
        }

        int wrongAttempts = (int) visibleAttempts
                .stream()
                .filter(s -> s.getStatus() == SubmissionStatus.WRONG)
                .filter(s -> s.getSubmittedAt().isBefore(correct.getSubmittedAt()))
                .count();
        long elapsedMinutes = 0;
        if (quest.getStartedAt() != null) {
            elapsedMinutes = Math.max(0, Duration.between(quest.getStartedAt(), correct.getSubmittedAt()).toMinutes());
        }

        int wrongAttemptPenalty =
                quest.getWrongAttemptPenaltyMinutes() != null
                ? quest.getWrongAttemptPenaltyMinutes()
                : 20;
        long penalty = elapsedMinutes + (long) wrongAttempts * wrongAttemptPenalty;
        return new RiddleScore(
                riddle.getId(),
                true,
                wrongAttempts,
                pendingAttempts,
                frozenAttempts,
                penalty
        );
    }


    private boolean isVisibleAt(Submission submission, Instant cutoff) {
        if (cutoff == null) {
            return true;
        }
        if (!submission.getSubmittedAt().isAfter(cutoff)) {
            return true;
        }
        return submission.getScoreboardRevealedAt() != null && !submission.getScoreboardRevealedAt().isBefore(cutoff);
    }
}