package br.edu.satc.backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.edu.satc.backend.models.UserEntity;
import br.edu.satc.backend.models.UserRole;


public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Page<UserEntity> findAllByActiveFalse(Pageable pageable);
    Page<UserEntity> findAllByActiveTrue(Pageable pageable);
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findById(UUID id);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.role = :role")
    long countByRole(UserRole role);

    boolean existsByEmailAndIdNot(String email, UUID id);
    
}
