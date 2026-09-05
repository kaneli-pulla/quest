package com.cityquest.quest.entity;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "riddles")
public class Riddle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private RiddleType type;

    private String referencePhotoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "riddle_correct_answers",
            joinColumns = @JoinColumn(name = "riddle_id")
    )
    @Column(name = "answer")
    private Set<String> correctAnswers = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RiddleType getType() {
        return type;
    }

    public void setType(RiddleType type) {
        this.type = type;
    }

    public String getReferencePhotoUrl() {
        return referencePhotoUrl;
    }

    public void setReferencePhotoUrl(String referencePhotoUrl) {
        this.referencePhotoUrl = referencePhotoUrl;
    }

    public Quest getQuest() {
        return quest;
    }

    public void setQuest(Quest quest) {
        this.quest = quest;
    }

    public Set<String> getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(Set<String> correctAnswers) {
        this.correctAnswers = correctAnswers;
    }
}
