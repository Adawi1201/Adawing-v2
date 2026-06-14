package cc.adabyte.blog.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class JwtUtil {
    private static final long EXPIRATION_DAYS = 7;

    public static String generateToken(String username, String secret) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(EXPIRATION_DAYS, ChronoUnit.DAYS)))
                .signWith(keyFor(secret))
                .compact();
    }

    public static Claims parseToken(String token, String secret) {
        return Jwts.parser().verifyWith(keyFor(secret)).build().parseSignedClaims(token).getPayload();
    }

    private static SecretKey keyFor(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String getUsername(String token, String secret) {
        return parseToken(token, secret).getSubject();
    }
}
