package iuh.fit.se.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
    
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private int jwtExpirationMs;
    
    @Value("${jwt.expirationDay}")
    private int jwtExpirationDay;

    
    
    
    public String generateTokenFromPhone(String phone) {
		// Tạo JWT token từ username
		// Sử dụng thư viện io.jsonwebtoken để tạo token
    	
    	Instant now = Instant.now();
        Instant expiry = now.plus(jwtExpirationDay, ChronoUnit.DAYS); // Khớp với maxAge cookie
    	
    	SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .subject(phone)
//            .setHeaderParam("typ", "JWT")
            .issuer("iuh.fit.se")
            .issuedAt(new Date())
            .expiration(new Date((new Date()).getTime() + expiry.toEpochMilli()))
            .signWith(key)
            .compact();
    }

    public String getPhoneFromToken(String token) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
