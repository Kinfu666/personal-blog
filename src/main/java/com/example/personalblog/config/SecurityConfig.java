package com.example.personalblog.config;

import java.io.IOException;
import java.util.LinkedHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var entryPoints = new LinkedHashMap<RequestMatcher, AuthenticationEntryPoint>();
        entryPoints.put(
                this::isJsonOperation,
                (request, response, error) -> jsonError(response, 401, "請先登入管理員帳號"));
        var entryPoint = new DelegatingAuthenticationEntryPoint(entryPoints);
        entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
        return http.authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/admin", "/admin/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .permitAll())
                .formLogin(
                        login ->
                                login.loginPage("/login")
                                        .defaultSuccessUrl("/admin", true)
                                        .failureUrl("/login?error"))
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(entryPoint)
                                        .accessDeniedHandler(
                                                (request, response, error) -> {
                                                    if (isJsonOperation(request))
                                                        jsonError(
                                                                response,
                                                                403,
                                                                "登入或表單驗證已過期，請重新整理後再試");
                                                    else response.sendError(403);
                                                }))
                .headers(
                        headers ->
                                headers.contentSecurityPolicy(
                                                csp ->
                                                        csp.policyDirectives(
                                                                "default-src 'self'; script-src"
                                                                    + " 'self'; style-src 'self';"
                                                                    + " img-src 'self'; object-src"
                                                                    + " 'none'; base-uri 'self';"
                                                                    + " frame-ancestors 'none';"
                                                                    + " form-action 'self'"))
                                        .referrerPolicy(
                                                ref ->
                                                        ref.policy(
                                                                ReferrerPolicyHeaderWriter
                                                                        .ReferrerPolicy
                                                                        .SAME_ORIGIN)))
                .build();
    }

    private boolean isJsonOperation(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return request.getMethod().equals("POST")
                && (path.equals("/admin/images") || path.equals("/admin/markdown/preview"));
    }

    private void jsonError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
