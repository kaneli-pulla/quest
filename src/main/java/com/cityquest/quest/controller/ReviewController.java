package com.cityquest.quest.controller;

import com.cityquest.quest.entity.Quest;
import com.cityquest.quest.entity.Submission;
import com.cityquest.quest.entity.SubmissionStatus;
import com.cityquest.quest.service.QuestService;
import com.cityquest.quest.service.RiddleService;
import com.cityquest.quest.service.SubmissionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/quests/{questId}/review")
public class ReviewController {
    private final QuestService questService;
    private final SubmissionService submissionService;
    private final RiddleService riddleService;

    public ReviewController(
            QuestService questService,
            SubmissionService submissionService,
            RiddleService riddleService
    ) {
        this.questService = questService;
        this.submissionService = submissionService;
        this.riddleService = riddleService;
    }

    @GetMapping
    public String showReview(@PathVariable Long questId, Model model) {
        Quest quest = questService.findById(questId);
        List<Submission> submissions = submissionService.findPhotoSubmissionsForQuest(questId);
        long pendingCount = submissions
                .stream()
                .filter(s -> s.getStatus() == SubmissionStatus.PENDING_REVIEW)
                .count();

        model.addAttribute("quest", quest);
        model.addAttribute("submissions", submissions);
        model.addAttribute("riddleNumbers", riddleService.getNumberMapForQuest(questId));
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("photoSubmissionCount", submissions.size());
        return "review";
    }

    @GetMapping("/count")
    @ResponseBody
    public long getSubmissionCount(@PathVariable Long questId) {
        questService.findById(questId);
        return submissionService.countPhotoSubmissionsForQuest(questId);
    }

    @PostMapping("/{submissionId}/accept")
    public String accept(@PathVariable Long questId, @PathVariable Long submissionId) {
        submissionService.reviewSubmission(questId, submissionId, true);
        return "redirect:/quests/" + questId + "/review";
    }

    @PostMapping("/{submissionId}/reject")
    public String reject(@PathVariable Long questId, @PathVariable Long submissionId) {
        submissionService.reviewSubmission(questId, submissionId, false);
        return "redirect:/quests/" + questId + "/review";
    }
}
