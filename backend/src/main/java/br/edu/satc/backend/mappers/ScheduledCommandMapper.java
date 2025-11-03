package br.edu.satc.backend.mappers;

import org.mapstruct.Mapper;

import br.edu.satc.backend.dtos.ScheduledCommandResponseDto;
import br.edu.satc.backend.models.ScheduledCommandEntity;

@Mapper(componentModel = "spring")
public interface ScheduledCommandMapper {
    ScheduledCommandResponseDto toDto(ScheduledCommandEntity scheduledCommandEntity);
}
