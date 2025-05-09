package com.kimtaeyang.mobidic.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kimtaeyang.mobidic.dto.JoinDto;
import com.kimtaeyang.mobidic.dto.LoginDto;
import com.kimtaeyang.mobidic.repository.MemberRepository;
import com.kimtaeyang.mobidic.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.UUID;

import static com.kimtaeyang.mobidic.code.AuthResponseCode.LOGIN_FAILED;
import static com.kimtaeyang.mobidic.code.AuthResponseCode.UNAUTHORIZED;
import static com.kimtaeyang.mobidic.code.GeneralResponseCode.INVALID_REQUEST_BODY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        memberRepository.deleteAll();
    }

    @DisplayName("[Auth][Integration] Join test")
    @Test
    @Transactional
    void joinTest() throws Exception {
        JoinDto.Request request = JoinDto.Request.builder()
                .email("test@test.com")
                .nickname("test")
                .password("testTest1")
                .build();

        //Success
        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email")
                        .value("test@test.com"))
                .andExpect(jsonPath("$.data.nickname")
                        .value("test"));

        request.setEmail("test@test");
        request.setNickname("1");
        request.setPassword("test");

        HashMap<String, String> expectedErrors = new HashMap<>();
        expectedErrors.put("email", "Invalid email pattern");
        expectedErrors.put("nickname", "Invalid nickname pattern");
        expectedErrors.put("password", "Invalid password pattern");

        //Email, nickname, password format fail
        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(INVALID_REQUEST_BODY.getMessage()))
                .andExpect(jsonPath("$.errors")
                        .value(expectedErrors));
    }

    @DisplayName("[Auth][Integration] Login test")
    @Test
    void loginTest() throws Exception {
        JoinDto.Request joinRequest = JoinDto.Request.builder()
                .email("test@test.com")
                .nickname("test")
                .password("testTest1")
                .build();

        //Join
        mockMvc.perform(post("/api/auth/join")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk());

        LoginDto.Request loginRequest = LoginDto.Request.builder()
                .email(joinRequest.getEmail())
                .password(joinRequest.getPassword())
                .build();

        //Login success
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(json).path("data").asText();

        assertThat(jwtUtil.validateToken(token));

        loginRequest.setPassword("wrongPassword");

        //Login fail invalid password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(LOGIN_FAILED.getMessage()));

        //Login fail invalid email
        loginRequest.setEmail("wrong@email.com");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(LOGIN_FAILED.getMessage()));

        //Login fail invalid email pattern
        loginRequest.setEmail("wrong");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(INVALID_REQUEST_BODY.getMessage()))
                .andExpect(jsonPath("$.errors.email")
                        .value("Invalid email pattern"));
    }

    @DisplayName("[Auth][Integration] Logout test")
    @Test
    void logoutTest() throws Exception {
        JoinDto.Request joinRequest = JoinDto.Request.builder()
                .email("test@test.com")
                .nickname("test")
                .password("testTest1")
                .build();

        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk());

        LoginDto.Request loginRequest = LoginDto.Request.builder()
                .email(joinRequest.getEmail())
                .password(joinRequest.getPassword())
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        //Logout success
        String loginJson = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(loginJson).path("data").asText();

        UUID memberId = jwtUtil.getIdFromToken(token);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(memberId.toString()));

        //Testing post method through invalid token
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));
    }

    @Test
    @DisplayName("[Auth][Integration] Withdraw test")
    public void withdrawTest() throws Exception {
        JoinDto.Request joinRequest = JoinDto.Request.builder()
                .email("test@test.com")
                .nickname("test")
                .password("testTest1")
                .build();

        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk());

        LoginDto.Request loginRequest = LoginDto.Request.builder()
                .email(joinRequest.getEmail())
                .password(joinRequest.getPassword())
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        //Withdraw success
        String loginJson = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(loginJson).path("data").asText();

        UUID memberId = jwtUtil.getIdFromToken(token);

        mockMvc.perform(patch("/api/user/withdraw/" + memberId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email")
                        .value(joinRequest.getEmail()))
                .andExpect(jsonPath("$.data.nickname")
                        .value(joinRequest.getNickname()))
                .andExpect(jsonPath("$.data.withdrawnAt")
                        .isNotEmpty());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));
    }
}
