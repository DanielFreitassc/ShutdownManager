package br.edu.satc.backend.dtos;

import org.hibernate.validator.constraints.Length;

import br.edu.satc.backend.configuration.OnCreate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRequestDto(
    @NotBlank(groups = OnCreate.class, message = "Nome não pode ser um campo em branco.") 
    String name,
    @NotBlank(groups = OnCreate.class, message = "Email não pode ser um campo em branco") 
    @Pattern(groups = OnCreate.class, regexp = "^(?=.*@)(?=.*\\.).+$", message = "Email deve conter '@' e '.'")
    String email,
    
    @Length(groups = OnCreate.class, min = 10, message = "A senha deve ter pelo menos 10 caracteres.")
    @Pattern(groups = OnCreate.class, regexp = "^(?=.*[a-z])(?=.*[A-Z]).*$", message = "A senha deve conter ao menos uma letra maiúscula e uma letra minúscula.")  
    @NotBlank(groups = OnCreate.class, message = "Senha não pode ser um campo em branco") 
    String password
) {
    
}
