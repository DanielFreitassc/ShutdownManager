package br.edu.satc.backend.controllers;


import br.edu.satc.backend.dtos.AgentRegisterRequestDto;
import br.edu.satc.backend.dtos.AgenteRegisterResponseDto;
import br.edu.satc.backend.models.AgentEntity;
import br.edu.satc.backend.services.AgentService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager") 
public class ManagerController {
    private final AgentService agentService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public AgenteRegisterResponseDto registerAgent(@RequestBody AgentRegisterRequestDto agentRegisterRequestDto) {   
        return agentService.registerAgent(agentRegisterRequestDto);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, String>> handleHeartbeat(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> payload) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Autorização ausente"));
        }
        
        String receivedKey = authHeader.substring(7); 
        String status = payload.getOrDefault("status", "unknown");
        String group = payload.getOrDefault("group", "default");
        
        Optional<AgentEntity> agentOpt = agentService.validateAgentByKey(receivedKey, status, group); 
        
        if (agentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Chave inválida"));
        }
        
        AgentEntity agent = agentOpt.get();
        System.out.println("[API] Heartbeat recebido do Agente '" + agent.getHostname() + "' (Grupo: "+agent.getAgentGroup()+", Status: " + status + ")"); 

        String command = agentService.getAndClearCommand(agent.getHostname());
        
        if (command != null) {
            return ResponseEntity.ok(Map.of("command", command));
        } else {
            return ResponseEntity.ok(Map.of("command", "ok"));
        }
    }

    @PostMapping("/admin/queue_command")
    public ResponseEntity<Map<String, String>> queueCommand(@RequestBody Map<String, String> payload) {
        String hostname = payload.get("hostname");
        String command = payload.get("command");
        
        if (hostname == null || command == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Payload inválido. 'hostname' e 'command' são obrigatórios."));
        }
        
        Optional<AgentEntity> agentOpt = agentService.findAgentByHostname(hostname);
        if (agentOpt.isEmpty()) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND)
                     .body(Map.of("erro", "Agente '" + hostname + "' não encontrado no banco de dados."));
        }

        agentService.queueCommand(hostname, command);
        System.out.println("[API-ADMIN] Comando '" + command + "' enfileirado para '" + hostname + "' via API.");

        return ResponseEntity.ok(Map.of(
            "status", "comando_enfileirado", 
            "target", hostname,
            "command", command
        ));
    }

    @PostMapping("/admin/queue_command_group")
    public ResponseEntity<Map<String, Object>> queueCommandForGroup(@RequestBody Map<String, String> payload) {
        String groupName = payload.get("group");
        String command = payload.get("command");

        if (groupName == null || command == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Payload inválido. 'group' e 'command' são obrigatórios."));
        }

        List<AgentEntity> agentsInGroup = agentService.findAgentsByGroup(groupName);

        if (agentsInGroup.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Nenhum agente encontrado no grupo '" + groupName + "'."));
        }

        for (AgentEntity agent : agentsInGroup) {
            agentService.queueCommand(agent.getHostname(), command);
        }
        
        System.out.println("[API-ADMIN] Comando '" + command + "' enfileirado para " + agentsInGroup.size() + " agentes no grupo '" + groupName + "'.");

        return ResponseEntity.ok(Map.of(
            "status", "comando_enfileirado_grupo",
            "target_group", groupName,
            "command", command,
            "agents_affected", agentsInGroup.size() 
        ));
    }

    @PostMapping("/admin/queue_command_all")
    public ResponseEntity<Map<String, Object>> queueCommandForAll(@RequestBody Map<String, String> payload) {
        String command = payload.get("command");

        if (command == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Payload inválido. 'command' é obrigatório."));
        }

        List<AgentEntity> allAgents = agentService.findAllAgents();

        if (allAgents.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Nenhum agente registrado no sistema."));
        }

        for (AgentEntity agent : allAgents) {
            agentService.queueCommand(agent.getHostname(), command);
        }
        
        System.out.println("[API-ADMIN] Comando '" + command + "' enfileirado para TODOS (" + allAgents.size() + ") os agentes.");

        return ResponseEntity.ok(Map.of(
            "status", "comando_enfileirado_todos",
            "command", command,
            "agents_affected", allAgents.size()
        ));
    }
}
