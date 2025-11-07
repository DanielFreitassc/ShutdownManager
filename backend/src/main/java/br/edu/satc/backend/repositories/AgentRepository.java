package br.edu.satc.backend.repositories;

import br.edu.satc.backend.models.AgentEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<AgentEntity, Long> {
    
    Optional<AgentEntity> findByAgentKey(String agentKey);
    Optional<AgentEntity> findByHostname(String hostname);

    List<AgentEntity> findAllByAgentGroup(String agentGroup);

    Page<AgentEntity> findAll(Pageable pageable);

    List<AgentEntity> findByStatusNotAndLastHeartbeatBefore(String status, LocalDateTime timestamp);

    List<AgentEntity> findByStatus(String status);
}
