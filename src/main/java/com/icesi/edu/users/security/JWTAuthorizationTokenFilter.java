package com.icesi.edu.users.security;

import com.icesi.edu.users.constant.ErrorConstants;
import com.icesi.edu.users.error.exception.UserDemoError;
import com.icesi.edu.users.error.exception.UserDemoException;
import com.icesi.edu.users.utils.JWTParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(1)
public class JWTAuthorizationTokenFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private static final String USER_ID_CLAIM_NAME = "userId";

    private static final String[] excludedPaths = {"POST /login"};


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (containsToken(request)) {
                String jwtToken = request.getHeader(AUTHORIZATION_HEADER).replace(TOKEN_PREFIX, StringUtils.EMPTY);
                Claims claims = JWTParser.decodeJWT(jwtToken);
                SecurityContext context = parseClaims(jwtToken, claims);
                SecurityContextHolder.setUserContext(context);
                filterChain.doFilter(request, response);
            } else {
                throw new UserDemoException(HttpStatus.UNAUTHORIZED, new UserDemoError("CODE_UD_13", ErrorConstants.CODE_UD_13));
            }
        } catch (JwtException e) {
            System.out.println("Error verifying JWT token: " + e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private SecurityContext parseClaims(String jwtToken, Claims claims) {
        String userId = claimKey(claims, USER_ID_CLAIM_NAME);

        SecurityContext context = new SecurityContext();
        try {
            context.setUserId(UUID.fromString(userId));
            context.setToken(jwtToken);
        } catch (IllegalArgumentException e) {
            throw new MalformedJwtException("Error parsing jwt");
        }
        return context;
    }

    private String claimKey(Claims claims, String key) {
        String value = (String) claims.get(key);
        return Optional.ofNullable(value).orElseThrow();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String methodPlusPath = request.getMethod() + " " + request.getRequestURI();
        return Arrays.stream(excludedPaths).anyMatch(path -> path.equalsIgnoreCase(methodPlusPath));
    }

    private boolean containsToken(HttpServletRequest request) {
        String authenticationHeader = request.getHeader(AUTHORIZATION_HEADER);
        return authenticationHeader != null && authenticationHeader.startsWith(TOKEN_PREFIX);
    }



}