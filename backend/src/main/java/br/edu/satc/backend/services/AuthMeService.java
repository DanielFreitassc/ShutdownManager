package br.edu.satc.backend.services;

import org.springframework.stereotype.Service;

import br.edu.satc.backend.dtos.UserResponseDto;
import br.edu.satc.backend.mappers.UserMapper;
import br.edu.satc.backend.models.UserEntity;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthMeService {
    private final UserMapper userMapper;

    public UserResponseDto auth(UserEntity userEntity) {
        return userMapper.toDto(userEntity);
    }
}
