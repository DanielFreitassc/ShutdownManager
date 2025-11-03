package br.edu.satc.backend.services;

import br.edu.satc.backend.dtos.ScheduleCommandDto;
import br.edu.satc.backend.dtos.ScheduledCommandResponseDto;
import br.edu.satc.backend.mappers.ScheduledCommandMapper;
import br.edu.satc.backend.models.ScheduledCommandEntity;
import br.edu.satc.backend.repositories.ScheduledCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledCommandService {

    private final ScheduledCommandRepository scheduledCommandRepository;
    private final AgentService agentService;
    private final ScheduledCommandMapper scheduledCommandMapper;

    public ScheduledCommandEntity scheduleCommand(ScheduleCommandDto dto) {
        ScheduledCommandEntity task = new ScheduledCommandEntity();
        task.setScheduledFor(dto.scheduledFor());

        if (dto.allHostsCommand() != null) {
            task.setCommand(dto.allHostsCommand().command());
            task.setTargetType("all");
            task.setTargetValue(null);
        } else if (dto.groupCommand() != null) {
            task.setCommand(dto.groupCommand().command());
            task.setTargetType("group");
            task.setTargetValue(dto.groupCommand().group());
        } else if (dto.hostCommand() != null) {
            task.setCommand(dto.hostCommand().command());
            task.setTargetType("host");
            task.setTargetValue(dto.hostCommand().hostname());
        } else {
            throw new IllegalArgumentException("Nenhum comando selecionado para agendamento.");
        }

        return scheduledCommandRepository.save(task);
    }

    public List<ScheduledCommandResponseDto> getSchedules() {
        return scheduledCommandRepository.findAll().stream().map(scheduledCommandMapper::toDto).toList();
    }


   @Scheduled(fixedRate = 60000)
    public void executeDueCommands() {
        LocalDateTime now = LocalDateTime.now();
        int toleranceMinutes = 2;

        List<ScheduledCommandEntity> dueCommands =
            scheduledCommandRepository.findByExecutedFalse();

        for (ScheduledCommandEntity cmd : dueCommands) {
            // Calcula diferença em minutos entre o horário atual e o agendado
            long minutesDiff = java.time.Duration.between(cmd.getScheduledFor(), now).toMinutes();

            // Executa apenas se dentro da margem [-tolerance, +tolerance]
            if (minutesDiff >= -toleranceMinutes && minutesDiff <= toleranceMinutes) {
                switch (cmd.getTargetType()) {
                    case "host" -> agentService.queueCommandForHost(
                        new br.edu.satc.backend.dtos.CommandHostUniqueRequestDto(cmd.getTargetValue(), cmd.getCommand())
                    );
                    case "group" -> agentService.queueCommandForGroup(
                        new br.edu.satc.backend.dtos.CommandGroupHostsRequestDto(cmd.getTargetValue(), cmd.getCommand())
                    );
                    case "all" -> agentService.queueCommandForAll(
                        new br.edu.satc.backend.dtos.CommandAllHostsRequestDto(cmd.getCommand())
                    );
                }

                cmd.setExecuted(true);
                scheduledCommandRepository.save(cmd);

                System.out.println("[SCHEDULER] Executado comando '" + cmd.getCommand() + "' agendado para " + cmd.getScheduledFor());
            }
        }
    }

}
