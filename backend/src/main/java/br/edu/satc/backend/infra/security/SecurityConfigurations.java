package br.edu.satc.backend.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfigurations {
    private final SecurityFilter securityFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize

                .requestMatchers(HttpMethod.POST,"/api/manager/register").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/manager/heartbeat").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/manager/admin/queue_command").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/manager/admin/queue_command_group").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/manager/admin/queue_command_all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/api/manager/admin/agents").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/manager/admin/schedule_command").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/api/manager/admin/schedule_command").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/api/manager/admin/agents/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,"/api/manager/admin/agents/{id}").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST,"/users").permitAll()
                .requestMatchers(HttpMethod.POST,"/users/{id}/activate").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/users").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/users/pending").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,"/users/{id}").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/users/inactives").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.GET,"/users/{id}").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,"/users/{id}").hasAnyRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,"/users/{id}").hasAnyRole("ADMIN")
                

                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/me").hasAnyRole("ADMIN")
                
                .requestMatchers("/error").anonymous()
                .anyRequest().denyAll()

                ).addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        // Trocar pelo ip da maquina que vai rodar o front
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.addAllowedMethod(HttpMethod.POST);
        configuration.addAllowedMethod(HttpMethod.GET);
        configuration.addAllowedMethod(HttpMethod.PUT);
        configuration.addAllowedMethod(HttpMethod.PATCH);
        configuration.addAllowedMethod(HttpMethod.DELETE);
        configuration.addAllowedHeader("*"); 

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
