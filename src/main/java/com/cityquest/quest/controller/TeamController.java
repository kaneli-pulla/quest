package com.cityquest.quest.controller;

import com.cityquest.quest.entity.Quest;
import com.cityquest.quest.entity.Team;
import com.cityquest.quest.service.QrCodeService;
import com.cityquest.quest.service.QuestService;
import com.cityquest.quest.service.TeamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequestMapping("/quests/{questId}/teams")
public class TeamController {
    private final TeamService teamService;
    private final QuestService questService;
    private final QrCodeService qrCodeService;

    public TeamController(
            TeamService teamService,
            QuestService questService,
            QrCodeService qrCodeService
    ) {
        this.teamService = teamService;
        this.questService = questService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/new")
    public String showCreateForm(@PathVariable Long questId, Model model) {
        Quest quest = questService.findById(questId);
        model.addAttribute("quest", quest);
        return "team-form";
    }

    @PostMapping
    public String createTeam(@PathVariable Long questId, @RequestParam String name) {
        teamService.create(questId, name);
        return "redirect:/quests/" + questId;
    }

    @GetMapping(value = "/{teamId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long questId, @PathVariable Long teamId) {
        Team team = teamService.findById(teamId);
        if (!team.getQuest().getId().equals(questId)) {
            return ResponseEntity.notFound().build();
        }
        String inviteUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/join/{token}")
                .buildAndExpand(team.getInviteToken())
                .toUriString();
        byte[] qrCode = qrCodeService.generatePng(inviteUrl, 320);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }
}