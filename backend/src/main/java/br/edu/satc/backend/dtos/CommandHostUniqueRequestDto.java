package br.edu.satc.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record CommandHostUniqueRequestDto(
    @NotBlank(message = "Hostname precisa ser indicado")
    String hostname,
    @NotBlank(message = "Um comando é necessário")
    String command
) {
    
}
