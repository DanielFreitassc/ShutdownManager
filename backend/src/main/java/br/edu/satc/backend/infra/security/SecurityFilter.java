package br.edu.satc.backend.infra.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import com.auth0.jwt.exceptions.JWTVerificationException;

import br.edu.satc.backend.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        boolean isPublicEndpoint =
                (path.equals("/users") && request.getMethod().equals("POST")) ||
                (path.equals("/auth/login") && request.getMethod().equals("POST")) ||
                (path.equals("/api/manager/register") && request.getMethod().equals("POST")) ||
                (path.equals("/api/manager/heartbeat") && request.getMethod().equals("POST"));

        var token = recoverToken(request);

        if (token != null) {
        if (!isPublicEndpoint) {
            try {
                var userId = tokenService.validateToken(token);
                UserDetails user = userRepository.findById(UUID.fromString(userId))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expirado"));

                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JWTVerificationException ex) {
                customAuthenticationEntryPoint.commence(request, response, 
                    new AuthenticationException("Token inválido ou expirado") {});
                return;
            } catch (IllegalArgumentException ex) {
                customAuthenticationEntryPoint.commence(request, response, 
                    new AuthenticationException("Formato do token inválido") {});
                return;
            } catch (ResponseStatusException ex) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Token expirado\"}");
                return;
            }
        }
    } else if (!isPublicEndpoint) {
        customAuthenticationEntryPoint.commence(request, response,
            new AuthenticationException("Token ausente") {});
        return;
    }

        
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.replace("Bearer ", "");
        }
    
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
    
        return null;
    }
}
