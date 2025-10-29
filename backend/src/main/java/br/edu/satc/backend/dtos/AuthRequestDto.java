package br.edu.satc.backend.dtos;

import br.edu.satc.backend.configuration.OnCreate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AuthRequestDto(
    @NotBlank(message = "Email é necessário")
    @Pattern(groups = OnCreate.class, regexp = "^(?=.*@)(?=.*\\.).+$", message = "Email deve conter '@' e '.'")
    String email,
    @NotBlank(message = "Senha é necessária")
    String password
) {
    
}
