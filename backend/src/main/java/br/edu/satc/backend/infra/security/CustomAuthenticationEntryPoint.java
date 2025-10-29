package br.edu.satc.backend.infra.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.satc.backend.dtos.MessageResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        response.resetBuffer(); 
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        MessageResponseDto messageResponseDto = new MessageResponseDto(
            determineErrorMessage(authException)
        );
        
        objectMapper.writeValue(response.getWriter(), messageResponseDto);
        response.flushBuffer();
    }

    private String determineErrorMessage(AuthenticationException ex) {
        String msg = ex.getMessage().toLowerCase();
    
        if (msg.contains("ausente")) {
            return "Token de autenticação é obrigatório";
        } else if (msg.contains("inválido") || msg.contains("expirado")) {
            return "Token inválido ou expirado";
        }
    
        return "Erro de autenticação";
    }
    
}
