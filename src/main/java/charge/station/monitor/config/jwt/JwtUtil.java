package charge.station.monitor.config.jwt;

import charge.station.monitor.dto.user.JwtUserInfo;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    private final Key key;
    private final long accessTokenExpTime;
    private final long refreshTokenExpTime;

    public JwtUtil(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access_time}") long accessTokenExpTime,
            @Value("${jwt.refresh_time}") long refreshTokenExpTime
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpTime = accessTokenExpTime;
        this.refreshTokenExpTime = refreshTokenExpTime;
    }

    /**
     * Access Token 생성
     * @param userInfo
     * @return JWT String
     */
    public String createAccessToken(JwtUserInfo userInfo) {
        Claims claims = Jwts.claims();
        claims.put("userId", userInfo.getUserId());
        claims.put("username", userInfo.getUsername());
        claims.put("userRole", userInfo.getRole());
        claims.put("managedRegions", userInfo.getManagedRegions());

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expiration = now.plusSeconds(accessTokenExpTime);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now.toInstant()))
                .setExpiration(Date.from(expiration.toInstant()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    public String createRefreshToken(JwtUserInfo userInfo) {
        Claims claims = Jwts.claims();
        claims.put("userId", userInfo.getUserId()); // ✅ 최소한의 정보만 저장

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expiration = now.plusSeconds(refreshTokenExpTime); // ✅ 유효기간 길게 설정

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now.toInstant()))
                .setExpiration(Date.from(expiration.toInstant()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 검증
     * @param token
     * @return IsValid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty.", e);
        }
        return false;
    }


    public Long getUserIdIfSignatureValid(String token) {
        try {
            // ✅ 만료된 토큰에서도 서명이 유효한지 검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)  // ✅ 서명 검증
                    .build()
                    .parseClaimsJws(token)  // ✅ 여기서 ExpiredJwtException 발생 가능
                    .getBody();

            return claims.get("userId", Long.class);
        } catch (JwtException e) {
            return null;  // ❌ 서명이 유효하지 않으면 userId를 반환하지 않음
        }
    }

    /**
     * Token에서 User ID 추출
     * @param token
     * @return User ID
     */
    public Long getUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public String getUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("userRole", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getManagedRegions(String token) {
        return (List<String>) parseClaims(token).get("managedRegions");
    }

    /**
     * JWT Claims 추출
     * @param token
     * @return Claims
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
