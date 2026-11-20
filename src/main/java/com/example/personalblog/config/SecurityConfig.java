package com.example.personalblog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var entryPoints =
                new java.util.LinkedHashMap<
                        org.springframework.security.web.util.matcher.RequestMatcher,
                        org.springframework.security.web.AuthenticationEntryPoint>();
        entryPoints.put(
                this::isJsonOperation,
                (request, response, error) -> jsonError(response, 401, "請先登入管理員帳號"));
        var entryPoint =
                new org.springframework.security.web.authentication
                        .DelegatingAuthenticationEntryPoint(entryPoints);
        entryPoint.setDefaultEntryPoint(
                new org.springframework.security.web.authentication
                        .LoginUrlAuthenticationEntryPoint("/login"));
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

    private boolean isJsonOperation(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return request.getMethod().equals("POST")
                && (path.equals("/admin/images") || path.equals("/admin/markdown/preview"));
    }

    private void jsonError(
            jakarta.servlet.http.HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
