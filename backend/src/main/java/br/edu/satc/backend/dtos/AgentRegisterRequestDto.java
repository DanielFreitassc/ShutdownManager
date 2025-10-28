package br.edu.satc.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record AgentRegisterRequestDto(
    @NotBlank(message = "Um agente precisa de um hostname")
    String hostname,
    @NotBlank(message = "Um agente precisa de um grupo.")
    String group
) {
    
}
