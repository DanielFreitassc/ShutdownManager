package br.edu.satc.backend.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "agents")
@Table(name = "agents")
public class AgentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String agentGroup;

    @Column(unique = true, nullable = false)
    private String hostname;

    @Column(unique = true, nullable = false)
    private String agentKey; 

    private String status;
    
    @CreationTimestamp
    private LocalDateTime lastHeartbeat;

    public AgentEntity(String hostname, String agentKey, String agentGroup) {
        this.hostname = hostname;
        this.agentKey = agentKey;
        this.agentGroup = agentGroup;
        this.status = "new";
        this.lastHeartbeat = LocalDateTime.now();
    }
}
