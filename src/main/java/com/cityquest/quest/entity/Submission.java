package com.cityquest.quest.entity;

import jakarta.persistence.*;

import java.time.Instant;


@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "riddle_id", nullable = false)
    private Riddle riddle;
    @Column(columnDefinition = "TEXT")
    private String answerText;
    private String photoUrl;
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;
    private Instant submittedAt;
    private Instant judgedAt;
    private Instant scoreboardRevealedAt;



    public Long getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Riddle getRiddle() {
        return riddle;
    }

    public void setRiddle(Riddle riddle) {
        this.riddle = riddle;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getJudgedAt() {
        return judgedAt;
    }

    public void setJudgedAt(Instant judgedAt) {
        this.judgedAt = judgedAt;
    }

    public Instant getScoreboardRevealedAt() {
        return scoreboardRevealedAt;
    }

    public void setScoreboardRevealedAt(Instant scoreboardRevealedAt) {
        this.scoreboardRevealedAt = scoreboardRevealedAt;
    }
}