package com.cityquest.quest.repository;

import com.cityquest.quest.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestRepository extends JpaRepository<Quest, Long> {
    //автоматическая реализация основных методов БД на Java
}