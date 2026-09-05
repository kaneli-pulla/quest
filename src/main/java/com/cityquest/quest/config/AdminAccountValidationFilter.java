package com.cityquest.quest.config;

import com.cityquest.quest.service.AdminUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminAccountValidationFilter
        extends OncePerRequestFilter {

    private final AdminUserService adminUserService;

    public AdminAccountValidationFilter(
            AdminUserService adminUserService
    ) {
        this.adminUserService = adminUserService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_ADMIN")
                )) {

            String username = authentication.getName();
            if (!adminUserService.existsByUsername(username)) {
                SecurityContextHolder.clearContext();
                HttpSession session =
                        request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect("/admin/login?removed");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}