package br.edu.satc.backend.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ScheduleCommandDto(
    @NotNull(message = "Data e hora do comando são obrigatórios")
    @Future(message = "A data deve estar no futuro")
    LocalDateTime scheduledFor,

    CommandAllHostsRequestDto allHostsCommand,
    CommandGroupHostsRequestDto groupCommand,
    CommandHostUniqueRequestDto hostCommand
) {}