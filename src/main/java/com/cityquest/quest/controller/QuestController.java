package com.cityquest.quest.controller;

import com.cityquest.quest.entity.Quest;
import com.cityquest.quest.service.QuestService;
import com.cityquest.quest.service.RiddleService;
import com.cityquest.quest.service.TeamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/quests")
public class QuestController {
    private final QuestService questService;
    private final RiddleService riddleService;
    private final TeamService teamService;

    public QuestController(
            QuestService questService,
            RiddleService riddleService,
            TeamService teamService
    ) {
        this.questService = questService;
        this.riddleService = riddleService;
        this.teamService = teamService;
    }

    @GetMapping
    public String showQuests(Model model) {
        model.addAttribute("quests", questService.findAll());
        return "quests";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("quest", new Quest());
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/quests");
        return "quest-form";
    }

    @PostMapping
    public String createQuest(
            @ModelAttribute Quest quest,
            @RequestParam(required = false)
            MultipartFile backgroundImage,
            @RequestParam(required = false)
            MultipartFile checkpointBoundaryImage
    ) {
        questService.create(quest, backgroundImage, checkpointBoundaryImage);
        return "redirect:/quests";
    }

    @GetMapping("/{id}")
    public String showQuest(@PathVariable Long id, Model model) {
        Quest quest = questService.findById(id);
        model.addAttribute("quest", quest);
        model.addAttribute("riddles", riddleService.findByQuestId(id));
        model.addAttribute("teams", teamService.findByQuestId(id));
        return "quest-details";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Quest quest = questService.findById(id);
        model.addAttribute("quest", quest);
        model.addAttribute("editing", true);
        model.addAttribute("formAction", "/quests/" + id + "/edit");
        return "quest-form";
    }

    @PostMapping("/{id}/edit")
    public String updateQuest(
            @PathVariable Long id,
            @RequestParam String name,

            @RequestParam(required = false, defaultValue = "")
            String description,

            @RequestParam(required = false, defaultValue = "")
            String rules,

            @RequestParam(required = false)
            MultipartFile backgroundImage,

            @RequestParam(required = false, defaultValue = "false")
            boolean removeBackground,

            @RequestParam(required = false)
            MultipartFile checkpointBoundaryImage,

            @RequestParam(required = false,defaultValue = "false")
            boolean removeCheckpointBoundaryImage
    ) {
        questService.updateQuest(
                id,
                name,
                description,
                rules,
                backgroundImage,
                removeBackground,
                checkpointBoundaryImage,
                removeCheckpointBoundaryImage
        );

        return "redirect:/quests/" + id;
    }

    @PostMapping("/{id}/start")
    public String startQuest(@PathVariable Long id) {
        questService.startQuest(id);
        return "redirect:/quests/" + id;
    }

    @PostMapping("/{id}/finish")
    public String finishQuest(@PathVariable Long id) {
        questService.finishQuest(id);
        return "redirect:/quests/" + id;
    }

    @PostMapping("/{id}/settings/penalty")
    public String changePenalty(@PathVariable Long id, @RequestParam Integer minutes) {
        questService.updateWrongAttemptPenalty(id, minutes);
        return "redirect:/quests/" + id;
    }

    @PostMapping("/{id}/scoreboard/freeze")
    public String freezeScoreboard(@PathVariable Long id) {
        questService.freezeScoreboard(id);
        return "redirect:/quests/" + id;
    }

    @PostMapping("/{id}/scoreboard/unfreeze")
    public String unfreezeScoreboard(@PathVariable Long id) {
        questService.unfreezeScoreboard(id);
        return "redirect:/quests/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteQuest(@PathVariable Long id) {
        questService.deleteQuest(id);
        return "redirect:/quests";
    }

    @PostMapping("/{id}/scoreboard/reveal-cell")
    public String revealScoreboardCell(
            @PathVariable Long id,
            @RequestParam Long teamId,
            @RequestParam Long riddleId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            questService.revealScoreboardCell(id, teamId, riddleId);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("scoreboardError", exception.getMessage());
        }
        return "redirect:/quests/" + id + "/results?unfreezeMode=true";
    }
}
