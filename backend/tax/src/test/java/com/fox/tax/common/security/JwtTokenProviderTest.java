package com.fox.tax.common.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Date;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = "9a2f8c4e6b0d71f3e8b9c2d4e6f8a0c2e4b6d8f0a2c4e6b8d0f2a4c6e8b0d2f4";
    private static final long EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = new JwtTokenProvider();
        setField(jwtTokenProvider, "jwtSecret", SECRET);
        setField(jwtTokenProvider, "jwtExpirationDate", EXPIRATION);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");

        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getUsername_shouldReturnCorrectUsername() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");

        String token = jwtTokenProvider.generateToken(authentication);
        String username = jwtTokenProvider.getUsername(token);

        assertEquals("admin", username);
    }

    @Test
    void validateToken_validToken_shouldReturnTrue() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");

        String token = jwtTokenProvider.generateToken(authentication);

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_expiredToken_shouldReturnFalse() {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 200000))
                .setExpiration(new Date(System.currentTimeMillis() - 100000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    @Test
    void validateToken_malformedToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.token"));
    }

    @Test
    void validateToken_emptyToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }
}
