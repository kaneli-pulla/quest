package com.cityquest.quest.controller;

import com.cityquest.quest.entity.*;
import com.cityquest.quest.service.RiddleService;
import com.cityquest.quest.service.SubmissionService;
import com.cityquest.quest.service.TeamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ParticipantController {
    private final TeamService teamService;
    private final RiddleService riddleService;
    private final SubmissionService submissionService;


    public ParticipantController(
            TeamService teamService,
            RiddleService riddleService,
            SubmissionService submissionService
    ) {
        this.teamService = teamService;
        this.riddleService = riddleService;
        this.submissionService = submissionService;
    }


    @GetMapping("/join/{token}")
    public String join(@PathVariable String token, HttpSession session) {
        Team team = teamService.findByInviteToken(token);
        session.setAttribute("teamId", team.getId());
        return "redirect:/play";
    }


    @GetMapping("/play")
    public String play(HttpSession session, Model model) {
        Long teamId = (Long) session.getAttribute("teamId");
        if (teamId == null) {
            return "redirect:/";
        }
        Team team = teamService.findById(teamId);
        Quest quest = team.getQuest();
        boolean questStarted = quest.getStatus() != QuestStatus.DRAFT;
        List<Riddle> riddles = questStarted
                ? riddleService.findByQuestId(quest.getId())
                : List.of();
        Map<Long, String> riddleStatuses = new HashMap<>();
        for (Riddle riddle : riddles) {
            SubmissionStatus status = submissionService.getCurrentStatus(teamId, riddle.getId());
            if (status != null) {
                riddleStatuses.put(riddle.getId(), status.name());
            }
        }
        model.addAttribute("team", team);
        model.addAttribute("quest", quest);
        model.addAttribute("questStarted", questStarted);
        model.addAttribute("riddles", riddles);
        model.addAttribute("riddleStatuses", riddleStatuses);
        return "play";
    }


    @GetMapping("/play/riddles/{riddleId}")
    public String showRiddle(@PathVariable Long riddleId, HttpSession session, Model model) {
        Long teamId = (Long) session.getAttribute("teamId");
        if (teamId == null) {
            return "redirect:/";
        }
        Team team = teamService.findById(teamId);
        Quest quest = team.getQuest();
        if (quest.getStatus() == QuestStatus.DRAFT) {
            return "redirect:/play";
        }
        Riddle riddle = riddleService.findById(riddleId);
        if (!riddle.getQuest().getId().equals(quest.getId())) {
            return "redirect:/play";
        }
        List<Submission> attempts = submissionService.findAttempts(teamId, riddleId);
        int riddleNumber = riddleService.getNumberInQuest(quest.getId(), riddleId);
        model.addAttribute("team", team);
        model.addAttribute("quest", quest);
        model.addAttribute("riddle", riddle);
        model.addAttribute("riddleNumber", riddleNumber);
        model.addAttribute("attempts", attempts);
        model.addAttribute("solved", submissionService.isSolved(teamId, riddleId));
        model.addAttribute("currentStatus", submissionService.getCurrentStatus(teamId, riddleId));
        return "riddle-play";
    }

    @GetMapping("/play/rules")
    public String showRules(HttpSession session, Model model) {
        Long teamId = (Long) session.getAttribute("teamId");
        if (teamId == null) {
            return "redirect:/";
        }
        Team team = teamService.findById(teamId);
        Quest quest = team.getQuest();
        model.addAttribute("team", team);
        model.addAttribute("quest", quest);
        return "rules";
    }

    @PostMapping("/play/riddles/{riddleId}/answer")
    public String submitAnswer(
            @PathVariable Long riddleId,
            @RequestParam String answer,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long teamId = (Long) session.getAttribute("teamId");
        if (teamId == null) {
            return "redirect:/";
        }
        try {
            Submission submission = submissionService.submitTextAnswer(teamId, riddleId, answer);
            if (submission.getStatus() == SubmissionStatus.CORRECT) {
                redirectAttributes.addFlashAttribute("successMessage", "Правильно! Загадка решена.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Ответ неверный. Попробуйте ещё раз.");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/play/riddles/" + riddleId;
    }


    @PostMapping("/play/riddles/{riddleId}/photo")
    public String submitPhotoAnswer(
            @PathVariable Long riddleId,
            @RequestParam(required = false, defaultValue = "") String answer,
            @RequestParam MultipartFile photo,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long teamId = (Long) session.getAttribute("teamId");
        if (teamId == null) {
            return "redirect:/";
        }
        try {
            Submission submission = submissionService.submitPhotoAnswer(teamId, riddleId, answer, photo);
            if (submission.getStatus() == SubmissionStatus.WRONG) {
                redirectAttributes.addFlashAttribute("errorMessage", "Текстовый ответ неверный. Попробуйте ещё раз.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Фото отправлено и ожидает проверки.");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/play/riddles/" + riddleId;
    }
}