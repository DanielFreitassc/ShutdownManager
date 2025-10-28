package br.edu.satc.backend.dtos;

import jakarta.validation.constraints.NotBlank;

public record CommandGroupHostsRequestDto(
    @NotBlank(message = "Indicar um grupo é necessário")
    String group,
    @NotBlank(message = "Um comando é necessário")
    String command
) {
    
}
