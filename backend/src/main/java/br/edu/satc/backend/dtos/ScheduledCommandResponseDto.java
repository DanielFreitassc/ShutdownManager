package br.edu.satc.backend.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduledCommandResponseDto(
    UUID id,
    String command,
    String targetType,
    String targetValue,
    LocalDateTime scheduledFor,
    boolean executed,
    LocalDateTime createdAt
) {
    
}
