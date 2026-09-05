package com.cityquest.quest.service;

import com.cityquest.quest.entity.AdminUser;
import com.cityquest.quest.repository.AdminUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AdminUserService implements UserDetailsService {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser admin = adminUserRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Администратор не найден")
                );
        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPasswordHash())
                .roles("ADMIN")
                .build();
    }

    @Transactional(readOnly = true)
    public boolean hasAdmins() {
        return adminUserRepository.count() > 0;
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        if (username == null) {
            return false;
        }
        return adminUserRepository.existsByUsernameIgnoreCase(
                username.trim()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUser> findAll() {
        return adminUserRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional
    public AdminUser createFirstAdmin(String displayName, String username, String password) {
        if (adminUserRepository.count() > 0) {
            throw new IllegalStateException("Первый администратор уже создан");
        }
        return createAdminInternal(displayName, username, password);
    }

    @Transactional
    public AdminUser createAdmin(String displayName, String username, String password) {
        return createAdminInternal(displayName, username, password);
    }

    @Transactional
    public void deleteAdmin(Long adminId, String currentUsername) {
        AdminUser admin = adminUserRepository
                .findById(adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Администратор не найден")
                );

        if (admin.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new IllegalStateException("Нельзя удалить собственный аккаунт");
        }
        if (adminUserRepository.count() <= 1) {
            throw new IllegalStateException("Нельзя удалить последнего администратора");
        }
        adminUserRepository.delete(admin);
    }

    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword) {
        AdminUser admin = adminUserRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Администратор не найден")
                );

        if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            throw new IllegalArgumentException("Текущий пароль введён неверно");
        }
        validatePassword(newPassword);
        if (passwordEncoder.matches(newPassword, admin.getPasswordHash())) {
            throw new IllegalArgumentException("Новый пароль совпадает с текущим");
        }
        admin.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private AdminUser createAdminInternal(String displayName, String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        if (adminUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Администратор с таким логином уже существует");
        }
        validatePassword(password);
        String normalizedDisplayName =
                displayName == null || displayName.isBlank()
                        ? normalizedUsername
                        : InputValidation.required(displayName,
                        "Имя администратора",
                            InputValidation.ADMIN_DISPLAY_NAME_MAX);
        AdminUser admin = new AdminUser();
        admin.setDisplayName(normalizedDisplayName);
        admin.setUsername(normalizedUsername);
        admin.setPasswordHash(passwordEncoder.encode(password));
        return adminUserRepository.save(admin);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Введите логин");
        }
        String normalized = username
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.length() < 3) {
            throw new IllegalArgumentException("Логин должен содержать минимум 3 символа");
        }
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("Логин слишком длинный");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Логин не должен содержать пробелы");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 8 символов");
        }
        if (password.length() > 200) {
            throw new IllegalArgumentException("Пароль слишком длинный");
        }
    }
}