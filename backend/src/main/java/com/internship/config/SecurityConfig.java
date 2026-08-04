package com.internship.config;

import com.internship.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.io.IOException;
import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil j) {
        jwtUtil = j;
    }

    @Bean
    public SecurityFilterChain fc(HttpSecurity http) throws Exception {
        http.cors(c -> c.configurationSource(ccs()))
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/auth/login", "/auth/register", "/auth/schools", "/certificates/verify", "/data/blockchain/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jf(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource ccs() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of("*"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OncePerRequestFilter jf() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                String h = req.getHeader("Authorization");
                if (h != null && h.startsWith("Bearer ")) {
                    String t = h.substring(7);
                    try {
                        if (jwtUtil.validateToken(t)) {
                            String u = jwtUtil.getUsernameFromToken(t);
                            String r = jwtUtil.getRoleFromToken(t);
                            Long uid = jwtUtil.getUserIdFromToken(t);
                            UsernamePasswordAuthenticationToken a = new UsernamePasswordAuthenticationToken(
                                    u, uid, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + r)));
                            SecurityContextHolder.getContext().setAuthentication(a);
                        }
                    } catch (Exception e) {
                        SecurityContextHolder.clearContext();
                    }
                }
                chain.doFilter(req, res);
            }
        };
    }
}
