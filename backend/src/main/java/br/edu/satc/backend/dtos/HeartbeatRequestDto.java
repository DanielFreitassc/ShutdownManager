package br.edu.satc.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record HeartbeatRequestDto(
    @NotBlank(message = "Status é necessário")
    String status,
    @NotBlank(message = "Grupo é ncessário")
    String group
) {
    
}
