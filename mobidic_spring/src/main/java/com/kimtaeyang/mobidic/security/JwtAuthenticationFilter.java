package com.kimtaeyang.mobidic.security;

import com.kimtaeyang.mobidic.entity.Member;
import com.kimtaeyang.mobidic.exception.ApiException;
import com.kimtaeyang.mobidic.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static com.kimtaeyang.mobidic.code.GeneralResponseCode.INVALID_TOKEN;


@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //JWT 파싱
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // "Bearer " 제거

        if (!jwtUtil.validateToken(token)) {
            throw new ApiException(INVALID_TOKEN);
        }

        //JWT Claim 에 포함된 정보만 갖는 Member 객체
        UUID id = jwtUtil.getIdFromToken(token);
        Member claim = Member.builder()
                .id(id)
                .build();

        //Security Context에 사용자 UUID 저장
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(claim, null, claim.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        //개발 중 테스트용 Logging
        log.info("JWT : {}", token);
        log.info("Member UUID : {}", id);

        filterChain.doFilter(request, response);
    }
}
