package br.edu.satc.backend.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.satc.backend.dtos.AgentRegisterRequestDto;
import br.edu.satc.backend.dtos.AgentResponseDto;
import br.edu.satc.backend.dtos.AgenteRegisterResponseDto;
import br.edu.satc.backend.dtos.CommandAllHostsRequestDto;
import br.edu.satc.backend.dtos.CommandGroupHostsRequestDto;
import br.edu.satc.backend.dtos.CommandHostUniqueRequestDto;
import br.edu.satc.backend.dtos.HeartbeatRequestDto;
import br.edu.satc.backend.dtos.HeartbeatResponseDto;
import br.edu.satc.backend.dtos.MessageResponseDto;
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

        return agentMapper.toRegisterDto(newAgent);
    }

    public HeartbeatResponseDto processHeartbeat(String agentKey, HeartbeatRequestDto dto) {
        AgentEntity agent = validateAgentByKey(agentKey, dto.status(), dto.group());
    
        String command = getAndClearCommand(agent.getHostname());
        
        if (command != null) {
            return new HeartbeatResponseDto(command); 
        } else {
            return new HeartbeatResponseDto("ok"); 
        }
    }

    private AgentEntity validateAgentByKey(String agentKey, String status, String group) {
        Optional<AgentEntity> agentOpt = agentRepository.findByAgentKey(agentKey);
        
        if (agentOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chave inválida");
        }

        AgentEntity agent = agentOpt.get();
        agent.setStatus(status);
        agent.setLastHeartbeat(LocalDateTime.now());
        
        if (group != null && !group.equals(agent.getAgentGroup())) {
            agent.setAgentGroup(group); 
        }
        
        agentRepository.save(agent);
        return agent; 
    }

    public MessageResponseDto queueCommandForHost(CommandHostUniqueRequestDto dto) {
        String hostname = dto.hostname();
        String command = dto.command();

        Optional<AgentEntity> agentOpt = agentRepository.findByHostname(hostname);
        if (agentOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agente '" + hostname + "' não encontrado no banco de dados.");
        }

        this.queueCommand(hostname, command); 

        return new MessageResponseDto("comando_enfileirado");
    }

    public MessageResponseDto queueCommandForGroup(CommandGroupHostsRequestDto dto) {
        String groupName = dto.group();
        String command = dto.command();

        List<AgentEntity> agentsInGroup = agentRepository.findAllByAgentGroup(groupName);
        if (agentsInGroup.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum agente encontrado no grupo '" + groupName + "'.");
        }

        for (AgentEntity agent : agentsInGroup) {
            this.queueCommand(agent.getHostname(), command);
        }
        
        return new MessageResponseDto("Shutdown solicitado ao grupo: " + groupName);
    }

    public MessageResponseDto queueCommandForAll(CommandAllHostsRequestDto dto) {
        String command = dto.command();

        List<AgentEntity> allAgents = agentRepository.findAll();
        if (allAgents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum agente registrado no sistema.");
        }

        for (AgentEntity agent : allAgents) {
            this.queueCommand(agent.getHostname(), command);
        }
        
        return new MessageResponseDto("Shutdown solicitado para todos os hosts");
    }
    
    public Page<AgentResponseDto> findAgents(Pageable pageable) {
       return agentRepository.findAll(pageable).map(agentMapper::toDto);
    }

    public AgentResponseDto findAgentByHost(Long id) {
        return agentMapper.toDto(findByHostOrThrow(id));
   
    }

    public MessageResponseDto deleteByHostId(Long id) {
        agentRepository.delete(findByHostOrThrow(id));
        return new MessageResponseDto("Agente removido com sucesso");
    }

    @Scheduled(fixedRate = 60000)
    public void checkOfflineAgents() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);

        List<AgentEntity> staleAgents = agentRepository.findByStatusNotAndLastHeartbeatBefore("offline", cutoffTime);

        if (staleAgents.isEmpty()) {
            return; 
        }

        System.out.println("[SCHEDULER] Encontrados " + staleAgents.size() + " agentes inativos. Marcando como 'offline'.");

        for (AgentEntity agent : staleAgents) {
            System.out.println("[SCHEDULER] - Host: " + agent.getHostname() + ", Último Heartbeat: " + agent.getLastHeartbeat());
            agent.setStatus("offline");
        }

        agentRepository.saveAll(staleAgents);
    }


    private AgentEntity findByHostOrThrow(Long id) {
        Optional<AgentEntity> agent = agentRepository.findById(id);
        if(agent.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Agente não encontrado");
        }
        return agent.get();
    }

    public void queueCommand(String hostname, String command) {
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
