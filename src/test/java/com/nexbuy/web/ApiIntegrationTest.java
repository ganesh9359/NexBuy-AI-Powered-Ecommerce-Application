package com.nexbuy.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexbuy.modules.auth.dto.LoginRequest;
import com.nexbuy.modules.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API Integration Tests")
class ApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── Auth Endpoints ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("Missing email returns 400")
        void register_missingEmail() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setPassword("Password1!");
            req.setConfirmPassword("Password1!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Invalid email format returns 400")
        void register_invalidEmail() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("not-an-email");
            req.setPassword("Password1!");
            req.setConfirmPassword("Password1!");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing password returns 400")
        void register_missingPassword() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("test@nexbuy.com");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Empty body returns 400")
        void login_emptyBody() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Wrong credentials return 401")
        void login_wrongCredentials() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("nobody@nexbuy.com");
            req.setPassword("WrongPass!");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── Product Endpoints ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /products")
    class ProductEndpointTests {

        @Test
        @DisplayName("Public catalog endpoint returns 200")
        void getCatalog_public() throws Exception {
            mockMvc.perform(get("/products"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Home feed endpoint returns 200")
        void getHomeFeed_public() throws Exception {
            mockMvc.perform(get("/products/home"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Non-existent product slug returns 404")
        void getProduct_notFound() throws Exception {
            mockMvc.perform(get("/products/this-product-does-not-exist-xyz"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Search endpoint returns 200 with query param")
        void search_withQuery() throws Exception {
            mockMvc.perform(get("/products/search").param("q", "laptop"))
                    .andExpect(status().isOk());
        }
    }

    // ── Admin Endpoints (Security) ────────────────────────────────────────────

    @Nested
    @DisplayName("Admin Security")
    class AdminSecurityTests {

        @Test
        @DisplayName("Admin dashboard without token returns 401 or 403")
        void adminDashboard_unauthenticated() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                            org.hamcrest.Matchers.is(401),
                            org.hamcrest.Matchers.is(403)
                    )));
        }

        @Test
        @DisplayName("Admin users endpoint without token returns 401 or 403")
        void adminUsers_unauthenticated() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                            org.hamcrest.Matchers.is(401),
                            org.hamcrest.Matchers.is(403)
                    )));
        }

        @Test
        @DisplayName("Admin products endpoint without token returns 401 or 403")
        void adminProducts_unauthenticated() throws Exception {
            mockMvc.perform(get("/admin/products"))
                    .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                            org.hamcrest.Matchers.is(401),
                            org.hamcrest.Matchers.is(403)
                    )));
        }

        @Test
        @DisplayName("Admin returns endpoint without token returns 401 or 403")
        void adminReturns_unauthenticated() throws Exception {
            mockMvc.perform(get("/admin/returns"))
                    .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                            org.hamcrest.Matchers.is(401),
                            org.hamcrest.Matchers.is(403)
                    )));
        }
    }

    // ── Cart Endpoints (Security) ─────────────────────────────────────────────

    @Nested
    @DisplayName("Cart Security")
    class CartSecurityTests {

        @Test
        @DisplayName("GET /cart without token returns 401 or 403")
        void getCart_unauthenticated() throws Exception {
            mockMvc.perform(get("/cart"))
                    .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                            org.hamcrest.Matchers.is(401),
                            org.hamcrest.Matchers.is(403)
                    )));
        }
    }

    // ── Order Endpoints (Security) ────────────────────────────────────────────

    @Nested
    @DisplayName("Order Security")
    class OrderSecurityTests {

        @Test
        @DisplayName("GET /orders without token returns 401 or 403")
        void getOrders_unauthenticated() throws Exception {
            mockMvc.perform(get("/orders"))
                    .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                            org.hamcrest.Matchers.is(401),
                            org.hamcrest.Matchers.is(403)
                    )));
        }
    }

    // ── Actuator ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Actuator")
    class ActuatorTests {

        @Test
        @DisplayName("Health endpoint is publicly accessible")
        void actuatorHealth_public() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }
}
