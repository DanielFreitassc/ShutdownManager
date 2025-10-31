package br.edu.satc.backend.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.satc.backend.dtos.AuthRequestDto;
import br.edu.satc.backend.dtos.AuthResponseDto;
import br.edu.satc.backend.dtos.UserResponseDto;
import br.edu.satc.backend.models.UserEntity;
import br.edu.satc.backend.services.AuthMeService;
import br.edu.satc.backend.services.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final AuthMeService authMeService;

    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody @Valid AuthRequestDto authRequestDto) {
        return authenticationService.login(authRequestDto);
    }  

    @GetMapping("/me")
    public UserResponseDto auth(@AuthenticationPrincipal UserEntity userEntity) {

        System.out.println(userEntity);
        return authMeService.auth(userEntity);
    }
}
