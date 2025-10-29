package br.edu.satc.backend.services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.satc.backend.dtos.MessageResponseDto;
import br.edu.satc.backend.dtos.UserRequestDto;
import br.edu.satc.backend.dtos.UserResponseDto;
import br.edu.satc.backend.mappers.UserMapper;
import br.edu.satc.backend.models.UserEntity;
import br.edu.satc.backend.models.UserRole;
import br.edu.satc.backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public MessageResponseDto create(UserRequestDto userRequestDto) {
        if(existsByEmail(userRequestDto.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Você já tem um cadastro");
        }

        UserEntity userEntity = userMapper.toEntity(userRequestDto);
        String passwordEncoded = passwordEncoder.encode(userRequestDto.password());
        userEntity.setPassword(passwordEncoded);
        userEntity.setRole(UserRole.ADMIN);
        userRepository.save(userEntity);
        return new MessageResponseDto("Sua conta foi criada. Ela será ativada após a aprovação de outro administrador.");
    }

    public Page<UserResponseDto> getToApproved(Pageable pageable) {
        return userRepository.findAllByActiveFalse(pageable).map(userMapper::toDto);
    }

    public Page<UserResponseDto> getAllApproved(Pageable pageable) {
        return userRepository.findAllByActiveTrue(pageable).map(userMapper::toDto);
    }
    
    public MessageResponseDto update(UUID id, UserRequestDto userRequestDto) {
        UserEntity userEntity = findUserOrThrow(id);

        if (userRequestDto.email() != null && !userRequestDto.email().isBlank()) {
            if (userRepository.existsByEmailAndIdNot(userRequestDto.email(), id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este email já está em uso por outro usuário");
            }
            userEntity.setEmail(userRequestDto.email());
        }

        if (userRequestDto.name() != null && !userRequestDto.name().isBlank()) {
            userEntity.setName(userRequestDto.name());
        }

        if (userRequestDto.password() != null && !userRequestDto.password().isBlank()) {
            userEntity.setPassword(passwordEncoder.encode(userRequestDto.password()));
        }

        userRepository.save(userEntity);
        return new MessageResponseDto("Usuário atualizado com sucesso");
    }

    public MessageResponseDto remove(UUID id) {
        UserEntity user = findUserOrThrow(id);

        if (user.getRole() == UserRole.ADMIN) {
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é permitido remover o único administrador restante.");
            }
        }

        userRepository.delete(user);
        return new MessageResponseDto("Usuário removido com sucesso");
    }

    public MessageResponseDto approved(UUID id) {
        UserEntity userEntity = findUserOrThrow(id);

        if (userEntity.getRole() == UserRole.ADMIN && userEntity.isActive()) {
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Não é permitido desativar o único administrador restante.");
            }
        }

        userEntity.setActive(!userEntity.isActive());
        userRepository.save(userEntity);

        String message = (userEntity.isActive()) ? "Usuário aprovado" : "Usuário desaprovado";
        return new MessageResponseDto(message);
    }


    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    private UserEntity findUserOrThrow(UUID id) {
        Optional<UserEntity> user = userRepository.findById(id);
        if(user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuário não encontrado");
        }
        return user.get();
    }
}
