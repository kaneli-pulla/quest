package com.cityquest.quest.controller;

import com.cityquest.quest.entity.Quest;
import com.cityquest.quest.entity.Riddle;
import com.cityquest.quest.entity.RiddleType;
import com.cityquest.quest.service.QuestService;
import com.cityquest.quest.service.RiddleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/quests/{questId}/riddles")
public class RiddleController {
    private final QuestService questService;
    private final RiddleService riddleService;

    public RiddleController(QuestService questService, RiddleService riddleService) {
        this.questService = questService;
        this.riddleService = riddleService;
    }

    @GetMapping("/new")
    public String showCreateForm(@PathVariable Long questId, Model model) {
        Quest quest = questService.findById(questId);
        Riddle riddle = new Riddle();
        riddle.setType(RiddleType.TEXT);

        model.addAttribute("quest", quest);
        model.addAttribute("riddle", riddle);
        model.addAttribute("types", RiddleType.values());
        model.addAttribute("correctAnswersText", "");
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/quests/" + questId + "/riddles");

        return "riddle-form";
    }

    @PostMapping
    public String createRiddle(
            @PathVariable Long questId,
            @ModelAttribute Riddle riddle,
            @RequestParam(required = false, defaultValue = "") String correctAnswersText,
            @RequestParam(required = false) MultipartFile referencePhoto
    ) {
        riddleService.create(questId, riddle, correctAnswersText, referencePhoto);
        return "redirect:/quests/" + questId;
    }

    @GetMapping("/{riddleId}/edit")
    public String showEditForm(
            @PathVariable Long questId,
            @PathVariable Long riddleId,
            Model model
    ) {
        Quest quest = questService.findById(questId);
        Riddle riddle = riddleService.findById(riddleId);
        if (!riddle.getQuest().getId().equals(questId)) {
            throw new IllegalArgumentException("Загадка относится к другому квесту");
        }
        model.addAttribute("quest", quest);
        model.addAttribute("riddle", riddle);
        model.addAttribute("types", RiddleType.values());
        model.addAttribute("correctAnswersText", String.join(System.lineSeparator(), riddle.getCorrectAnswers()));
        model.addAttribute("editing", true);
        model.addAttribute("formAction", "/quests/" + questId + "/riddles/" + riddleId + "/edit");
        return "riddle-form";
    }

    @PostMapping("/{riddleId}/edit")
    public String updateRiddle(
            @PathVariable Long questId,
            @PathVariable Long riddleId,
            @ModelAttribute Riddle riddle,
            @RequestParam(required = false, defaultValue = "") String correctAnswersText,
            @RequestParam(required = false) MultipartFile referencePhoto
    ) {
        riddleService.update(questId, riddleId, riddle, correctAnswersText, referencePhoto);
        return "redirect:/quests/" + questId;
    }

    @PostMapping("/{riddleId}/delete")
    public String deleteRiddle(
            @PathVariable Long questId,
            @PathVariable Long riddleId
    ) {
        riddleService.delete(questId, riddleId);
        return "redirect:/quests/" + questId;
    }
}