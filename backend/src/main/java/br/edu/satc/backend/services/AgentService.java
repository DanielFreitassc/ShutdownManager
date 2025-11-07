package br.edu.satc.backend.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public AgenteRegisterResponseDto registerAgent(AgentRegisterRequestDto dto) {
        // Verifica se já existe um agente com a mesma chave
        Optional<AgentEntity> existingAgent = agentRepository.findByAgentKey(dto.agentKey());
        
        if (existingAgent.isPresent()) {
            AgentEntity agent = existingAgent.get();
            // Se o agente estiver pendente, só retorna mensagem informando
            if ("pending".equals(agent.getStatus())) {
                return agentMapper.toRegisterDto(agent);
            }

            // Se já aprovado, apenas retorna os dados
            return agentMapper.toRegisterDto(agent);
        }

        // Cria um novo registro pendente
        AgentEntity newAgent = new AgentEntity();
        newAgent.setHostname(dto.hostname());
        newAgent.setAgentGroup(dto.group());
        newAgent.setAgentKey(dto.agentKey());
        newAgent.setStatus("pending"); // ⚠️ pendente até admin aprovar
        newAgent.setLastHeartbeat(LocalDateTime.now());

        agentRepository.save(newAgent);

        return agentMapper.toRegisterDto(newAgent);
    }



    public HeartbeatResponseDto processHeartbeat(String agentKey, HeartbeatRequestDto dto) {
        Optional<AgentEntity> agentOpt = agentRepository.findByAgentKey(agentKey);
        
        if (agentOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chave inválida");
        }

        AgentEntity agent = agentOpt.get();

        // ⚠️ Bloqueia se não aprovado
        if (!"approved".equals(agent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agente pendente ou não aprovado");
        }

        agent.setStatus(dto.status());
        agent.setLastHeartbeat(LocalDateTime.now());
        if (dto.group() != null && !dto.group().equals(agent.getAgentGroup())) {
            agent.setAgentGroup(dto.group());
        }
        agentRepository.save(agent);

        String command = getAndClearCommand(agent.getHostname());
        return new HeartbeatResponseDto(command != null ? command : "ok");
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

    public MessageResponseDto approveAgent(Long id) {
        AgentEntity agent = findByHostOrThrow(id);

        if ("approved".equalsIgnoreCase(agent.getStatus())) {
            return new MessageResponseDto("Agente já está aprovado.");
        }

        agent.setStatus("approved");
        agentRepository.save(agent);
        return new MessageResponseDto("Agente aprovado com sucesso!");
    }

    public List<AgentResponseDto> getPendingAgents() {
        return agentRepository.findByStatus("pending").stream().map(agentMapper::toDto).toList();
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
