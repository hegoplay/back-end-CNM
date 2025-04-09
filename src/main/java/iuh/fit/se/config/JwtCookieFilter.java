package iuh.fit.se.config;

import java.io.IOException;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.lang.Collections;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtCookieFilter extends OncePerRequestFilter {

	@Value("${jwt.secret}")
	private String secretKey;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
        
        // 1. Lấy cookie từ request
        Cookie[] cookies = request.getCookies();
        String token = null;
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("authToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 2. Verify JWT
        if (token != null) {
            try {
            	
            	SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

            	Claims claims = Jwts.parser()
            	    .verifyWith(key)  // Thay setSigningKey() bằng verifyWith()
            	    .build()          // Bắt buộc phải gọi build()
            	    .parseSignedClaims(token)  // Thay parseClaimsJws() bằng parseSignedClaims()
            	    .getPayload();    // Thay getBody() bằng getPayload()
                
                // 3. Tạo Authentication object và lưu vào SecurityContext
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(),
                    null,
                    Collections.emptyList()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                
            } catch (Exception e) {
                // Xử lý token không hợp lệ
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

}
