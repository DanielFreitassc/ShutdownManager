package br.edu.satc.backend.dtos;

import java.time.LocalDateTime;


public record AgentResponseDto(
    String id,
    String group,
    String hostname,
    String key,
    String status,
    LocalDateTime lastHeartbeat
) {


}
