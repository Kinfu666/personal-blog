package com.example.personalblog.service;

import com.example.personalblog.entity.User;
import com.example.personalblog.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService implements UserDetailsService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Environment environment;
    private final String initialPassword;

    public AdminService(
            UserRepository users,
            PasswordEncoder encoder,
            Environment environment,
            @Value("${app.admin.initial-password:}") String initialPassword) {
        this.users = users;
        this.encoder = encoder;
        this.environment = environment;
        this.initialPassword = initialPassword;
    }

    @Transactional
    public void initialize() {
        if (users.findByUsername("admin").isPresent()) return;
        boolean demo =
                environment.acceptsProfiles(Profiles.of("demo"))
                        && !environment.acceptsProfiles(Profiles.of("prod"));
        String password = initialPassword;
        if (demo && password.isBlank()) password = "DemoBlog!2026";
        if (password.length() < 12 || (!demo && password.equals("DemoBlog!2026"))) {
            throw new IllegalStateException("正式模式首次啟動必須設定 BLOG_ADMIN_PASSWORD，至少 12 字，且不得使用展示密碼。");
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("初始密碼超過 BCrypt 的 72 位元組上限。");
        }
        users.saveAndFlush(new User("admin", encoder.encode(password)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user =
                users.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("帳號或密碼不正確"));
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
    }
}
