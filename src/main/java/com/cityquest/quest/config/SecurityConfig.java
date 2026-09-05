package com.cityquest.quest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AdminAccountValidationFilter adminAccountValidationFilter
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/",
                                "/join/**",
                                "/play/**",
                                "/css/**",
                                "/uploads/**",
                                "/sw.js",
                                "/favicon.ico",
                                "/error"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/admin/login",
                                "/admin/setup"
                        )
                        .permitAll()

                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/quests/**")
                        .hasRole("ADMIN")

                        .anyRequest()
                        .denyAll()
                )

                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/quests", true)
                        .failureUrl("/admin/login?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .permitAll()
                )

                .addFilterAfter(
                        adminAccountValidationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}