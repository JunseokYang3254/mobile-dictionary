package com.kimtaeyang.mobidic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.kimtaeyang.mobidic.code.GeneralResponseCode.BAD_REQUEST;

public class LoginDto {
    private static final String EMAIL_ERROR_MESSAGE = BAD_REQUEST.getMessage();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Request{
        @NotBlank
        @Email(message = "Invalid email pattern")
        private String email;
        @NotBlank
        private String password;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response{
        private String token;
    }
}
