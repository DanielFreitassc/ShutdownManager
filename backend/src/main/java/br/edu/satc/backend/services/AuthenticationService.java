package br.edu.satc.backend.services;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.satc.backend.dtos.AuthRequestDto;
import br.edu.satc.backend.dtos.AuthResponseDto;
import br.edu.satc.backend.infra.security.TokenService;
import br.edu.satc.backend.models.UserEntity;
import br.edu.satc.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    
    private static final int MAX_LOGIN_ATTEMPTS = 4;

    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public AuthResponseDto login(AuthRequestDto authRequestDto) {
        UserEntity user = findUserOrThrow(authRequestDto);

        validateUserStatus(user);

        if (passwordEncoder.matches(authRequestDto.password(), user.getPassword())) {
            user.resetLoginAttempts();
            userRepository.save(user);
            String token = tokenService.generateToken(user);
            return new AuthResponseDto(token);
        } else {
            handleFailedLogin(user);
            return null;
        }
    }

    private UserEntity findUserOrThrow(AuthRequestDto dto) {
        Optional<UserEntity> user = userRepository.findByEmail(dto.email());
        if(user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"E-mail ou senha inválidos.");
        }
        return user.get();
    }

    private void validateUserStatus(UserEntity user) {
        if (user.isAccountLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"A conta está bloqueada. Tente novamente mais tarde.");
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sua conta ainda não está ativada.");
        }
    }

    private void handleFailedLogin(UserEntity user) {
        user.incrementLoginAttempts();

        if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.lockAccount();
        }

        userRepository.save(user);

        if (user.isAccountLocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Conta bloqueada por excesso de tentativas.");
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"E-mail ou senha inválidos.");
        }
    }
}
