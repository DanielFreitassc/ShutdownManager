package br.edu.satc.backend.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.satc.backend.models.ScheduledCommandEntity;

public interface ScheduledCommandRepository extends JpaRepository<ScheduledCommandEntity, UUID> {
    List<ScheduledCommandEntity> findByExecutedFalse();

    List<ScheduledCommandEntity> findByExecutedFalseAndScheduledForBefore(LocalDateTime time);
}
