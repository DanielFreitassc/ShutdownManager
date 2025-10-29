package br.edu.satc.backend.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.satc.backend.dtos.AuthRequestDto;
import br.edu.satc.backend.dtos.AuthResponseDto;
import br.edu.satc.backend.services.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/login")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping
    public AuthResponseDto login(@RequestBody @Valid AuthRequestDto authRequestDto) {
        return authenticationService.login(authRequestDto);
    }  
}
