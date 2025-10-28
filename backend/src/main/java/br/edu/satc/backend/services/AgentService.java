package br.edu.satc.backend.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.satc.backend.dtos.AgentRegisterRequestDto;
import br.edu.satc.backend.dtos.AgenteRegisterResponseDto;
import br.edu.satc.backend.mappers.AgentMapper;
import br.edu.satc.backend.models.AgentEntity;
import br.edu.satc.backend.repositories.AgentRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AgentService {
    private final AgentRepository agentRepository;
    private final Map<String, String> commandQueue = new ConcurrentHashMap<>();
    private final AgentMapper agentMapper;

    public AgenteRegisterResponseDto registerAgent(AgentRegisterRequestDto agentRegisterRequestDto) {
        Optional<AgentEntity> existingAgent = agentRepository.findByHostname(agentRegisterRequestDto.hostname());

        if (existingAgent.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Agente já cadastrado");
        }

        String newKey = UUID.randomUUID().toString();
        
        AgentEntity newAgent = agentMapper.toEntity(agentRegisterRequestDto);
        newAgent.setAgentKey(newKey);
        newAgent.setStatus("new");

        agentRepository.save(newAgent);

        return agentMapper.toDto(newAgent);
    }

    public Optional<AgentEntity> validateAgentByKey(String agentKey, String status, String group) {
        Optional<AgentEntity> agentOpt = agentRepository.findByAgentKey(agentKey);
        
        if (agentOpt.isPresent()) {
            AgentEntity agent = agentOpt.get();
            agent.setStatus(status);
            agent.setLastHeartbeat(LocalDateTime.now());
            if (group != null && !group.equals(agent.getAgentGroup())) {
                agent.setAgentGroup(group); 
                System.out.println("[SERVICE] Agente '" + agent.getHostname() + "' atualizou seu grupo para '" + group + "' via heartbeat.");
            }
            agentRepository.save(agent);
            return Optional.of(agent);
        }
        
        return Optional.empty();
    }

    
    public Optional<AgentEntity> findAgentByHostname(String hostname) {
        return agentRepository.findByHostname(hostname);
   
    }

    public void queueCommand(String hostname, String command) {
        System.out.println("[SERVICE] Admin enfileirou comando '" + command + "' para o agente '" + hostname + "'");
        commandQueue.put(hostname, command);
    }

    public List<AgentEntity> findAgentsByGroup(String agentGroup) {
        return agentRepository.findAllByAgentGroup(agentGroup);
    }

    public List<AgentEntity> findAllAgents() {
        return agentRepository.findAll();
    }
    
    public String getAndClearCommand(String hostname) {
        return commandQueue.remove(hostname);
    }
}
