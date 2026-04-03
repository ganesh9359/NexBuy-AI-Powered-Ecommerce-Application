package com.nexbuy.web;

import com.nexbuy.config.CorsConfig;
import com.nexbuy.security.JwtFilter;
import com.nexbuy.security.JwtUtil;
import com.nexbuy.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppEntryController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        JwtFilter.class,
        JwtUtil.class,
        AppEntryControllerTest.TestConfig.class
})
@TestPropertySource(properties = {
        "app.frontend-url=http://localhost:4200",
        "security.jwt.secret=01234567890123456789012345678901"
})
class AppEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootRedirectsToFrontend() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4200"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        UserDetailsService userDetailsService() {
            return username -> User.withUsername(username)
                    .password("password")
                    .authorities(Collections.emptyList())
                    .build();
        }
    }
}
