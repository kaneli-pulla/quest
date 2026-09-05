package com.cityquest.quest.controller;

import com.cityquest.quest.service.AdminUserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/quests";
    }

    /*
     * Первый запуск приложения.
     */
    @GetMapping("/setup")
    public String showSetup() {
        if (adminUserService.hasAdmins()) {
            return "redirect:/admin/login";
        }
        return "admin-setup";
    }

    @PostMapping("/setup")
    public String createFirstAdmin(
            @RequestParam String displayName,
            @RequestParam String username,
            @RequestParam String password,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.createFirstAdmin(displayName, username, password);
            return "redirect:/admin/login?created";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/admin/setup";
        }
    }

    /*
     * Вход для админов.
     */
    @GetMapping("/login")
    public String showLogin(Authentication authentication) {
        if (!adminUserService.hasAdmins()) {
            return "redirect:/admin/setup";
        }
        if (isAuthenticated(authentication)) {
            return "redirect:/quests";
        }
        return "admin-login";
    }

    /*
     * Управление администраторами.
     */
    @GetMapping("/users")
    public String showAdmins(Authentication authentication, Model model) {
        model.addAttribute("admins", adminUserService.findAll());
        model.addAttribute("currentUsername", authentication.getName());
        return "admin-users";
    }

    @PostMapping("/users")
    public String createAdmin(
            @RequestParam String displayName,
            @RequestParam String username,
            @RequestParam String password,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.createAdmin(displayName, username, password);
            redirectAttributes.addFlashAttribute("successMessage", "Администратор добавлен");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{adminId}/delete")
    public String deleteAdmin(
            @PathVariable Long adminId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.deleteAdmin(adminId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Доступ администратора удалён");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/account/password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.changeOwnPassword(authentication.getName(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Пароль изменён");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}