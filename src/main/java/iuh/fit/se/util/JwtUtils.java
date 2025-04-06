package iuh.fit.se.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
    
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private int jwtExpirationMs;

    
    
    
    public String generateTokenFromUsername(String username) {
		// Tạo JWT token từ username
		// Sử dụng thư viện io.jsonwebtoken để tạo token
    	SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(key)
            .compact();
    }

    // Các method validate token...
    // Phương thức tạo Authentication từ JWT
//    public Authentication getAuthentication(String jwt) {
//        String username = getUsernameFromToken(jwt);
//        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//        return new UsernamePasswordAuthenticationToken(
//            userDetails, null, userDetails.getAuthorities()
//        );
//    }
}
