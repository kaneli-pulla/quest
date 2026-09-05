package com.cityquest.quest.controller;

import com.cityquest.quest.entity.Quest;
import com.cityquest.quest.entity.QuestStatus;
import com.cityquest.quest.entity.Team;
import com.cityquest.quest.service.QuestService;
import com.cityquest.quest.service.RiddleService;
import com.cityquest.quest.service.ScoringService;
import com.cityquest.quest.service.TeamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ResultsController {

    private final TeamService teamService;
    private final QuestService questService;
    private final RiddleService riddleService;
    private final ScoringService scoringService;

    public ResultsController(
            TeamService teamService,
            QuestService questService,
            RiddleService riddleService,
            ScoringService scoringService
    ) {
        this.teamService = teamService;
        this.questService = questService;
        this.riddleService = riddleService;
        this.scoringService = scoringService;
    }

    @GetMapping("/play/results")
    public String showPublicResults(
            HttpSession session,
            Model model
    ) {
        Long teamId = (Long) session.getAttribute("teamId");
        if (teamId == null) {
            return "redirect:/";
        }

        Team team = teamService.findById(teamId);
        Quest quest = team.getQuest();

        if (quest.getStatus() == QuestStatus.DRAFT) {
            return "redirect:/play";
        }

        model.addAttribute("quest", quest);

        model.addAttribute(
                "riddles",
                riddleService.findByQuestId(quest.getId())
        );

        model.addAttribute(
                "scores",
                scoringService.calculateScores(
                        quest,
                        true
                )
        );

        model.addAttribute("adminView", false);

        /*
         * ВАЖНО.
         *
         * results.html общий для администратора
         * и участника, поэтому переменная должна
         * существовать и в публичном режиме.
         */
        model.addAttribute("unfreezeMode", false);

        model.addAttribute(
                "currentTeamId",
                teamId
        );

        return "results";
    }


    @GetMapping("/quests/{questId}/results")
    public String showAdminResults(
            @PathVariable Long questId,

            @RequestParam(
                    required = false,
                    defaultValue = "false"
            )
            boolean unfreezeMode,

            Model model
    ) {
        Quest quest =
                questService.findById(questId);

        boolean effectiveUnfreezeMode =
                unfreezeMode
                        && quest.isScoreboardFrozen();

        model.addAttribute(
                "quest",
                quest
        );

        model.addAttribute(
                "riddles",
                riddleService.findByQuestId(questId)
        );

        model.addAttribute(
                "scores",
                scoringService.calculateScores(
                        quest,
                        effectiveUnfreezeMode
                )
        );

        model.addAttribute(
                "adminView",
                true
        );

        model.addAttribute(
                "unfreezeMode",
                effectiveUnfreezeMode
        );

        return "results";
    }
}