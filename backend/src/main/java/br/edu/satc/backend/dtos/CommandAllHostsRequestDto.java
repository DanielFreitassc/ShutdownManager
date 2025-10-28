package br.edu.satc.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record CommandAllHostsRequestDto(
    @NotBlank(message = "Um comando é necessário")
    String command
) {
    
}
