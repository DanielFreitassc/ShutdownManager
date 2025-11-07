package br.edu.satc.backend.models;

import java.time.LocalDateTime;
import java.util.UUID;

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

@Entity
@Table(name = "scheduled_commands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String command; 

    @Column(nullable = false)
    private String targetType; 

    @Column(nullable = true)
    private String targetValue; 

    @Column(nullable = false)
    private LocalDateTime scheduledFor;

    @Column(nullable = false)
    private boolean executed = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}