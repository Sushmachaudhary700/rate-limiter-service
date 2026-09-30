package com.ratelimiter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.dto.RateLimitCheckRequest;
import com.ratelimiter.dto.RateLimitCheckResponse;
import com.ratelimiter.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RateLimitController.class)
class RateLimitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RateLimitService rateLimitService;

    @Test
    void returns200WhenRequestAllowed() throws Exception {
        when(rateLimitService.check(eq("client-a"), eq("/orders"), anyInt()))
                .thenReturn(RateLimitCheckResponse.builder()
                        .allowed(true).remaining(9).retryAfterMs(0).algorithm("TOKEN_BUCKET").build());

        RateLimitCheckRequest req = new RateLimitCheckRequest("client-a", "/orders", 1);

        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(header().string("X-RateLimit-Remaining", "9"));
    }

    @Test
    void returns429WhenRequestDenied() throws Exception {
        when(rateLimitService.check(eq("client-a"), eq("/orders"), anyInt()))
                .thenReturn(RateLimitCheckResponse.builder()
                        .allowed(false).remaining(0).retryAfterMs(1500).algorithm("TOKEN_BUCKET").build());

        RateLimitCheckRequest req = new RateLimitCheckRequest("client-a", "/orders", 1);

        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(header().string("X-RateLimit-Retry-After-Ms", "1500"));
    }

    @Test
    void returns400WhenClientIdMissing() throws Exception {
        RateLimitCheckRequest req = new RateLimitCheckRequest("", "/orders", 1);

        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
