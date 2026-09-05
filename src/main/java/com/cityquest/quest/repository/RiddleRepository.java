package com.cityquest.quest.repository;

import com.cityquest.quest.entity.Riddle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiddleRepository extends JpaRepository<Riddle, Long> {
    List<Riddle> findAllByQuestIdOrderByIdAsc(Long questId);
}