package br.edu.satc.backend.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.satc.backend.dtos.MessageResponseDto;
import br.edu.satc.backend.dtos.UserRequestDto;
import br.edu.satc.backend.dtos.UserResponseDto;
import br.edu.satc.backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public MessageResponseDto create(@RequestBody @Valid UserRequestDto userRequestDto) {
        return userService.create(userRequestDto);
    }
    
    @GetMapping
    public Page<UserResponseDto> getAllApproved(Pageable pageable) {
        return userService.getAllApproved(pageable);
    }

    @GetMapping("/pending")
    public Page<UserResponseDto> getToApproved(Pageable pageable) {
        return userService.getToApproved(pageable);
    }
    
    @PostMapping("/{id}/activate")
    public MessageResponseDto approved(@PathVariable UUID id) {
        return userService.approved(id);
    }

    @PatchMapping("/{id}")
    public MessageResponseDto update(@PathVariable UUID id,@RequestBody @Valid UserRequestDto userRequestDto) {
        return userService.update(id, userRequestDto);
    }

    @DeleteMapping("/{id}")
    public MessageResponseDto remove(@PathVariable UUID id) {
        return userService.remove(id);
    }
}
