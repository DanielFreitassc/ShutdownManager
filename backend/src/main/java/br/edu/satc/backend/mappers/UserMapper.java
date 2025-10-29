package br.edu.satc.backend.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.edu.satc.backend.dtos.UserRequestDto;
import br.edu.satc.backend.dtos.UserResponseDto;
import br.edu.satc.backend.models.UserEntity;


@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDto toDto(UserEntity userEntity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "loginAttempts", ignore = true)
    @Mapping(target = "lockoutExpiration", ignore = true)
    UserEntity toEntity(UserRequestDto userRequestDto);
    
}
