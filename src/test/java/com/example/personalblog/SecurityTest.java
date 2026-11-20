package com.example.personalblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.personalblog.repository.UserRepository;
import com.example.personalblog.service.AdminService;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class SecurityTest {
    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) throws Exception {
        r.add("app.data-dir", Files.createTempDirectory("blog-security-")::toString);
    }

    @Autowired MockMvc mvc;
    @Autowired AdminService admins;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void loginSuccessAndFailure() throws Exception {
        mvc.perform(formLogin().user("admin").password("DemoBlog!2026"))
                .andExpect(redirectedUrl("/admin"));
        mvc.perform(formLogin().user("admin").password("wrong"))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login?error"))
                .andExpect(content().string(Matchers.containsString("帳號或密碼不正確")));
    }

    @Test
    void adminIsProtectedAndCsrfIsRequired() throws Exception {
        mvc.perform(get("/admin/posts")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/posts")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/posts").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(logout()).andExpect(redirectedUrl("/"));
    }

    @Test
    void initialPasswordIsHashedAndNeverOverwritten() {
        String hash = users.findByUsername("admin").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2a$12$");
        assertThat(encoder.matches("DemoBlog!2026", hash)).isTrue();
        admins.initialize();
        assertThat(users.findByUsername("admin").orElseThrow().getPasswordHash()).isEqualTo(hash);
    }

    @Test
    void loginChangesSessionIdAndLogoutInvalidatesSession() throws Exception {
        var session = new MockHttpSession();
        String before = session.getId();
        var result =
                mvc.perform(
                                post("/login")
                                        .session(session)
                                        .with(csrf())
                                        .param("username", "admin")
                                        .param("password", "DemoBlog!2026"))
                        .andExpect(redirectedUrl("/admin"))
                        .andReturn();
        var authenticated = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(authenticated.getId()).isNotEqualTo(before);
        mvc.perform(post("/logout").session(authenticated).with(csrf()))
                .andExpect(redirectedUrl("/"));
        assertThat(authenticated.isInvalid()).isTrue();
    }
}
