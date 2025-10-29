package br.edu.satc.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner; 
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import br.edu.satc.backend.models.UserEntity;
import br.edu.satc.backend.models.UserRole;
import br.edu.satc.backend.repositories.UserRepository;

import org.springframework.core.annotation.Order; 

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(2)
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${user.mock.username}")
    private String adminUsername;

    @Value("${user.mock.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Executando AdminUserInitializer...");

        try {
            if (!userService.existsByEmail(adminUsername)) {
                String encryptedPassword = new BCryptPasswordEncoder().encode(adminPassword);

                UserEntity admin = new UserEntity();
                admin.setName("Administrador");
                admin.setEmail(adminUsername);
                admin.setPassword(encryptedPassword);
                admin.setRole(UserRole.ADMIN);
                admin.setActive(true);
                userRepository.save(admin);
            } else {
                System.out.println("Admin criado");
            }

        } finally {
            System.out.println("Admin concluído.");
        }

    }
}
