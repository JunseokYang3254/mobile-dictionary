package com.kimtaeyang.mobidic.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kimtaeyang.mobidic.dto.AddVocabDto;
import com.kimtaeyang.mobidic.dto.JoinDto;
import com.kimtaeyang.mobidic.dto.LoginDto;
import com.kimtaeyang.mobidic.dto.UpdateVocabDto;
import com.kimtaeyang.mobidic.repository.MemberRepository;
import com.kimtaeyang.mobidic.repository.VocabRepository;
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

import java.util.UUID;

import static com.kimtaeyang.mobidic.code.AuthResponseCode.UNAUTHORIZED;
import static com.kimtaeyang.mobidic.code.GeneralResponseCode.INVALID_REQUEST_BODY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class VocabIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private VocabRepository vocabRepository;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        vocabRepository.deleteAll();
    }

    private final JoinDto.Request joinRequest = JoinDto.Request.builder()
            .email("test@test.com")
            .nickname("test")
            .password("testTest1")
            .build();

    private final LoginDto.Request loginRequest = LoginDto.Request.builder()
            .email(joinRequest.getEmail())
            .password(joinRequest.getPassword())
            .build();

    @Test
    @DisplayName("[Vocab][Integration] Add vocab test")
    void addVocabTest() throws Exception {
        String token = loginAndGetToken();
        UUID memberId = jwtUtil.getIdFromToken(token);

        AddVocabDto.Request addVocabRequest = AddVocabDto.Request.builder()
                .title("title")
                .description("description")
                .build();

        //Success
        mockMvc.perform(post("/api/vocab/" + memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addVocabRequest))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.title")
                                .value(addVocabRequest.getTitle()))
                .andExpect(
                        jsonPath("$.data.description")
                                .value(addVocabRequest.getDescription()))
                .andExpect(
                        jsonPath("$.data.id")
                                .isNotEmpty());

        //Fail without token
        mockMvc.perform(post("/api/vocab/" + memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addVocabRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value(UNAUTHORIZED.getMessage()));

        //Fail with unauthorized token
        mockMvc.perform(post("/api/vocab/" + memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(UUID.randomUUID()))
                        .content(objectMapper.writeValueAsString(addVocabRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value(UNAUTHORIZED.getMessage()));

        //Fail with unauthorized token
        mockMvc.perform(post("/api/vocab/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(addVocabRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value(UNAUTHORIZED.getMessage()));

        //Fail with invalid pattern
        addVocabRequest.setTitle(UUID.randomUUID().toString());
        addVocabRequest.setDescription(UUID.randomUUID().toString() + UUID.randomUUID());
        mockMvc.perform(post("/api/vocab/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(addVocabRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(INVALID_REQUEST_BODY.getMessage()))
                .andExpect(
                        jsonPath("$.errors.title")
                                .value("Invalid title pattern"))
                .andExpect(
                        jsonPath("$.errors.description")
                                .value("Invalid description pattern"));
    }

    @Test
    @DisplayName("[Vocab][Integration] Get vocabs by member id test")
    void getVocabsByMemberIdTest() throws Exception {
        String token = loginAndGetToken();
        UUID memberId = jwtUtil.getIdFromToken(token);

        AddVocabDto.Request addVocabRequest = AddVocabDto.Request.builder()
                .title("title")
                .description("description")
                .build();

        mockMvc.perform(post("/api/vocab/" + memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addVocabRequest))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        //Success
        mockMvc.perform(get("/api/vocab/all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .param("uId", memberId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title")
                        .value(addVocabRequest.getTitle()))
                .andExpect(jsonPath("$.data[0].description")
                        .value(addVocabRequest.getDescription()));

        //Fail without token
        mockMvc.perform(get("/api/vocab/all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("uId", memberId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Fail with unauthorized token
        mockMvc.perform(get("/api/vocab/all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(UUID.randomUUID()))
                        .param("uId", memberId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Fail with no resource
        mockMvc.perform(get("/api/vocab/all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("uId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));
    }

    @Test
    @DisplayName("[Vocab][Integration] Get vocab by id test")
    void getVocabByIdTest() throws Exception {
        String token = loginAndGetToken();
        UUID memberId = jwtUtil.getIdFromToken(token);

        AddVocabDto.Request addVocabRequest = AddVocabDto.Request.builder()
                .title("title")
                .description("description")
                .build();

        MvcResult addResult = mockMvc.perform(post("/api/vocab/" + memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addVocabRequest))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String json = addResult.getResponse().getContentAsString();
        AddVocabDto.Response addVocabResponse = objectMapper.convertValue(
                objectMapper.readTree(json).path("data"), AddVocabDto.Response.class
        );

        //Success
        mockMvc.perform(get("/api/vocab/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .param("vId", addVocabResponse.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(addVocabResponse.getId().toString()))
                .andExpect(jsonPath("$.data.memberId")
                        .value(memberId.toString()))
                .andExpect(jsonPath("$.data.title")
                        .value(addVocabRequest.getTitle()))
                .andExpect(jsonPath("$.data.description")
                        .value(addVocabRequest.getDescription()))
                .andExpect(jsonPath("$.data.createdAt")
                        .isNotEmpty());

        //Fail without token
        mockMvc.perform(get("/api/vocab/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("vId", addVocabResponse.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Fail with unauthorized token
        mockMvc.perform(get("/api/vocab/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(UUID.randomUUID()))
                        .param("vId", addVocabResponse.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Fail with no resource
        mockMvc.perform(get("/api/vocab/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .param("vId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));
    }

    @Test
    @DisplayName("[Vocab][Integration] Update vocab test")
    void updateVocabTest() throws Exception {
        String token = loginAndGetToken();
        UUID memberId = jwtUtil.getIdFromToken(token);

        AddVocabDto.Request addVocabRequest = AddVocabDto.Request.builder()
                .title("title")
                .description("description")
                .build();

        MvcResult addResult = mockMvc.perform(post("/api/vocab/" + memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addVocabRequest))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String json = addResult.getResponse().getContentAsString();
        AddVocabDto.Response addVocabResponse = objectMapper.convertValue(
                objectMapper.readTree(json).path("data"), AddVocabDto.Response.class
        );

        UpdateVocabDto.Request updateVocabRequest = UpdateVocabDto.Request.builder()
                .title("title2")
                .description("description2")
                .build();

        //Fail without token
        mockMvc.perform(patch("/api/vocab/" + addVocabResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVocabRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Fail with unauthorized token
        mockMvc.perform(patch("/api/vocab/" + addVocabResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(UUID.randomUUID()))
                        .content(objectMapper.writeValueAsString(updateVocabRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Fail with no resource
        mockMvc.perform(patch("/api/vocab/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(updateVocabRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Success
        mockMvc.perform(patch("/api/vocab/" + addVocabResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(updateVocabRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title")
                        .value(updateVocabRequest.getTitle()))
                .andExpect(jsonPath("$.data.description")
                        .value(updateVocabRequest.getDescription()));

        //Fail with invalid pattern
        updateVocabRequest.setTitle(UUID.randomUUID().toString());
        updateVocabRequest.setDescription(UUID.randomUUID().toString() + UUID.randomUUID());
        System.out.println(updateVocabRequest.getDescription() + ", " + updateVocabRequest.getDescription().length());
        mockMvc.perform(patch("/api/vocab/" + addVocabResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(updateVocabRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(INVALID_REQUEST_BODY.getMessage()))
                .andExpect(jsonPath("$.errors.title")
                        .value("Invalid title pattern"))
                .andExpect(jsonPath("$.errors.description")
                        .value("Invalid description pattern"));
    }

    @Test
    @DisplayName("[Vocab][Integration] Delete vocab test")
    void deleteVocabTest() throws Exception {
        String token = loginAndGetToken();
        UUID memberId = jwtUtil.getIdFromToken(token);

        AddVocabDto.Request addVocabRequest = AddVocabDto.Request.builder()
                .title("title")
                .description("description")
                .build();

        MvcResult addResult = mockMvc.perform(post("/api/vocab/" + memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addVocabRequest))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String json = addResult.getResponse().getContentAsString();
        AddVocabDto.Response addVocabResponse = objectMapper.convertValue(
                objectMapper.readTree(json).path("data"), AddVocabDto.Response.class
        );

        //Fail without token
        mockMvc.perform(delete("/api/vocab/" + addVocabResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("vocabId", addVocabResponse.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Fail with unauthorized token
        mockMvc.perform(delete("/api/vocab/" + addVocabResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(UUID.randomUUID()))
                        .param("vocabId", addVocabResponse.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Fail with no resource
        mockMvc.perform(delete("/api/vocab/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .param("vocabId", addVocabResponse.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));

        //Success
        mockMvc.perform(delete("/api/vocab/" + addVocabResponse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .param("vocabId", addVocabResponse.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(addVocabResponse.getId().toString()))
                .andExpect(jsonPath("$.data.title")
                        .value(addVocabRequest.getTitle()))
                .andExpect(jsonPath("$.data.description")
                        .value(addVocabRequest.getDescription()))
                .andExpect(jsonPath("$.data.createdAt")
                        .isNotEmpty())
                .andExpect(jsonPath("$.data.memberId")
                        .isNotEmpty());

        mockMvc.perform(get("/api/vocab/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .param("vId", addVocabResponse.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value(UNAUTHORIZED.getMessage()));
    }

    private String loginAndGetToken() throws Exception {
        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String json = loginResult.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data").asText();
    }
}
// Resource api integration test convention
//Success
// -> OK
//Fail without token
// -> UNAUTHORIZED
//Fail with unauthorized token
// -> UNAUTHORIZED
//Fail with no resource
// -> UNAUTHORIZED
//Fail with invalid pattern
// -> INVALID_REQUEST_BODY