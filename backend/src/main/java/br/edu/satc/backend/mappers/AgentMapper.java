package br.edu.satc.backend.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.edu.satc.backend.dtos.AgentRegisterRequestDto;
import br.edu.satc.backend.dtos.AgenteRegisterResponseDto;
import br.edu.satc.backend.models.AgentEntity;

@Mapper(componentModel = "spring")
public interface AgentMapper {
    @Mapping(target = "agentGroup",source = "group")
    @Mapping(target = "agentKey", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastHeartbeat", ignore = true)
    AgentEntity toEntity(AgentRegisterRequestDto agentRegisterRequestDto);

    @Mapping(target = "key",source = "agentKey")
    @Mapping(target = "id", expression = "java(String.format(\"%03d\", agentEntity.getId()))")
    AgenteRegisterResponseDto toDto(AgentEntity agentEntity);
}
