package com.cityquest.quest.repository;

import com.cityquest.quest.entity.Submission;
import com.cityquest.quest.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findAllByTeamIdAndRiddleIdOrderBySubmittedAtAsc(Long teamId, Long riddleId);

    List<Submission> findAllByTeamIdOrderBySubmittedAtAsc(Long teamId);

    boolean existsByTeamIdAndRiddleIdAndStatus(Long teamId, Long riddleId, SubmissionStatus status);

    long countByTeamIdAndRiddleIdAndStatus(Long teamId, Long riddleId, SubmissionStatus status);

    @Query("""
            select s
            from Submission s
            join fetch s.team
            join fetch s.riddle r
            where r.quest.id = :questId
              and s.photoUrl is not null
            order by s.submittedAt desc
            """)
    List<Submission> findPhotoSubmissionsForQuest(@Param("questId") Long questId);

    @Query("""
            select count(s)
            from Submission s
            where s.riddle.quest.id = :questId
              and s.photoUrl is not null
            """)
    long countPhotoSubmissionsForQuest(@Param("questId") Long questId);

    List<Submission> findAllByRiddleId(Long riddleId);
    List<Submission> findAllByRiddleIdOrderBySubmittedAtAsc(Long riddleId);

    @Query("""
        select s
        from Submission s
        where s.riddle.quest.id = :questId
        """)
    List<Submission> findAllByQuestId(@Param("questId") Long questId);

}