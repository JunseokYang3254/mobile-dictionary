package com.kimtaeyang.mobidic.exception;

import com.kimtaeyang.mobidic.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

import static com.kimtaeyang.mobidic.code.AuthResponseCode.LOGIN_FAILED;
import static com.kimtaeyang.mobidic.code.AuthResponseCode.UNAUTHORIZED;
import static com.kimtaeyang.mobidic.code.GeneralResponseCode.INTERNAL_SERVER_ERROR;
import static com.kimtaeyang.mobidic.code.GeneralResponseCode.INVALID_REQUEST_BODY;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> methodArgumentNotValidException(
            final MethodArgumentNotValidException e, final HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                        e, request.getRequestURI(), e.getMessage());

        HashMap<String, String> errors = new HashMap<>();

        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ErrorResponse.toResponseEntity(INVALID_REQUEST_BODY, errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> badCredentials(
            BadCredentialsException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());
        return ErrorResponse.toResponseEntity(LOGIN_FAILED, null);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> authorizationDenied(
            AuthorizationDeniedException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());
        return ErrorResponse.toResponseEntity(UNAUTHORIZED, null);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> apiException(
            ApiException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(e.getResponseCode(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> exception(
            Exception e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e, request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(INTERNAL_SERVER_ERROR, null);
    }
}
