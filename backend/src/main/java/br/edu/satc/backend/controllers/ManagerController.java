package br.edu.satc.backend.controllers;


import br.edu.satc.backend.dtos.AgentRegisterRequestDto;
import br.edu.satc.backend.dtos.AgentResponseDto;
import br.edu.satc.backend.dtos.AgenteRegisterResponseDto;
import br.edu.satc.backend.dtos.CommandAllHostsRequestDto;
import br.edu.satc.backend.dtos.CommandGroupHostsRequestDto;
import br.edu.satc.backend.dtos.CommandHostUniqueRequestDto;
import br.edu.satc.backend.dtos.HeartbeatRequestDto;
import br.edu.satc.backend.dtos.HeartbeatResponseDto;
import br.edu.satc.backend.dtos.MessageResponseDto;
import br.edu.satc.backend.dtos.ScheduleCommandDto;
import br.edu.satc.backend.dtos.ScheduledCommandResponseDto;
import br.edu.satc.backend.services.AgentService;
import br.edu.satc.backend.services.ScheduledCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager") 
public class ManagerController {
    private final AgentService agentService;
    private final ScheduledCommandService scheduledCommandService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public AgenteRegisterResponseDto registerAgent(@RequestBody @Valid AgentRegisterRequestDto agentRegisterRequestDto) {   
        return agentService.registerAgent(agentRegisterRequestDto);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<HeartbeatResponseDto> handleHeartbeat(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid HeartbeatRequestDto heartbeatRequestDto) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autorização ausente");
        }
        
        String receivedKey = authHeader.substring(7); 
        
        HeartbeatResponseDto response = agentService.processHeartbeat(receivedKey, heartbeatRequestDto);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/queue_command")
    public MessageResponseDto queueCommand(@RequestBody @Valid CommandHostUniqueRequestDto commandHostUniqueRequestDto) {
        return agentService.queueCommandForHost(commandHostUniqueRequestDto);
    }

    @PostMapping("/admin/queue_command_group")
    public MessageResponseDto queueCommandForGroup(@RequestBody @Valid CommandGroupHostsRequestDto commandHostUniqueRequestDto) {
        return agentService.queueCommandForGroup(commandHostUniqueRequestDto);
    }

    @PostMapping("/admin/queue_command_all")
    public MessageResponseDto queueCommandForAll(@RequestBody @Valid CommandAllHostsRequestDto commandHostUniqueRequestDto) {
        return agentService.queueCommandForAll(commandHostUniqueRequestDto);
    }

    @GetMapping("/admin/agents")
    public Page<AgentResponseDto> findAgentsHandle(Pageable pageable) {
        return agentService.findAgents(pageable);
    }

    @PostMapping("/admin/schedule_command")
    public ResponseEntity<MessageResponseDto> scheduleCommand(
            @RequestBody @Valid ScheduleCommandDto dto) {
        scheduledCommandService.scheduleCommand(dto);
        return ResponseEntity.ok(new MessageResponseDto(
            "Comando agendado para " + dto.scheduledFor()
        ));
    }

    @GetMapping("/admin/schedule_command")
    public List<ScheduledCommandResponseDto> getSchedules() {
        return scheduledCommandService.getSchedules();
    }   

    @DeleteMapping("/admin/agents/{id}")
    public MessageResponseDto deleteByHostId(@PathVariable Long id) {
        return agentService.deleteByHostId(id);
    }

    @GetMapping("/admin/agents/{id}")
    public AgentResponseDto findAgentByHost(@PathVariable Long id) {
        return agentService.findAgentByHost(id);
    }

}
