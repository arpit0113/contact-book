package com.exercise.contactBook.filter;

import com.exercise.contactBook.model.User;
import com.exercise.contactBook.service.CustomUserDetailsService;
import com.exercise.contactBook.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class Jwtfilter extends OncePerRequestFilter {
    private JwtUtil jwtUtil;
    private CustomUserDetailsService userDetailsService ;

    public Jwtfilter(JwtUtil jwtUtil , CustomUserDetailsService userDetailsService){
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtUtil.extractEmail(token);

            if(email != null && SecurityContextHolder.getContext().getAuthentication() == null){
                UserDetails user = userDetailsService.loadUserByUsername(email);

                if(jwtUtil.isTokenValid(token , email) && user.isEnabled()){
                    UsernamePasswordAuthenticationToken uPAToken = new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
                    uPAToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(uPAToken);
                }
            }
        } catch (Exception ex) {
            // Invalid, malformed, or expired token: leave the request unauthenticated
            // instead of throwing and returning a raw 500. Downstream Spring Security
            // will reject access to protected endpoints as normal (401/403).
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request,response);
    }
}
