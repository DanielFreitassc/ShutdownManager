package br.edu.satc.backend.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.edu.satc.backend.models.UserRole;
public record UserResponseDto(
    UUID id,
    String name,
    String email,
    UserRole role,
    @JsonFormat(pattern = "dd/MM/yyyy")
    LocalDateTime createdAt
) {
    
}
