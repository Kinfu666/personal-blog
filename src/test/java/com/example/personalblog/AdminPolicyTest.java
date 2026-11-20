package com.example.personalblog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.personalblog.entity.User;
import com.example.personalblog.repository.UserRepository;
import com.example.personalblog.service.AdminService;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

class AdminPolicyTest {
    @Test
    void productionRejectsMissingOrDemoPassword() {
        var users = mock(UserRepository.class);
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        var env = new MockEnvironment();
        env.setActiveProfiles("prod", "demo");
        for (String password : new String[] {"", "short", "DemoBlog!2026"}) {
            var service = new AdminService(users, new BCryptPasswordEncoder(4), env, password);
            assertThatThrownBy(service::initialize).isInstanceOf(IllegalStateException.class);
        }
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void existingUserIsNeverReplacedAndLongPasswordsAreRejected() {
        var users = mock(UserRepository.class);
        when(users.findByUsername("admin")).thenReturn(Optional.of(new User("admin", "existing")));
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        new AdminService(users, new BCryptPasswordEncoder(4), env, "").initialize();
        verify(users, never()).saveAndFlush(any());
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                new AdminService(
                                                users,
                                                new BCryptPasswordEncoder(4),
                                                env,
                                                "密".repeat(25))
                                        .initialize())
                .isInstanceOf(IllegalStateException.class);
    }
}
