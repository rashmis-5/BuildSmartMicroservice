package com.buildsmart.finance.service.impl;

import com.buildsmart.finance.client.PmNotificationClient;
import com.buildsmart.finance.client.dto.PmNotificationDto;
import com.buildsmart.finance.dto.response.AssignedTaskResponse;
import com.buildsmart.finance.dto.response.AssignedTaskSyncResult;
import com.buildsmart.finance.entity.AssignedTask;
import com.buildsmart.finance.entity.enums.AssignedTaskStatus;
import com.buildsmart.finance.repository.AssignedTaskRepository;
import com.buildsmart.finance.service.AssignedTaskService;
import com.buildsmart.finance.util.IdGenerator;
import com.buildsmart.finance.util.JwtUtil;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssignedTaskServiceImpl implements AssignedTaskService {

    private final AssignedTaskRepository assignedTaskRepository;
    private final PmNotificationClient pmNotificationClient;
    private final JwtUtil jwtUtil;

    @Override
    public AssignedTaskSyncResult syncTasksFromPm(String authorizationHeader) {
        String token = jwtUtil.extractTokenFromHeader(authorizationHeader);
        String currentUserId = jwtUtil.extractUserId(token);
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new IllegalStateException("Could not resolve userId from JWT");
        }

        List<PmNotificationDto> pmNotifications;
        try {
            pmNotifications = pmNotificationClient.getNotificationsTo(currentUserId, authorizationHeader);
        } catch (FeignException e) {
            log.warn("Could not reach project-service to sync tasks: {}", e.getMessage());
            pmNotifications = List.of();
        }

        int newCount = 0;
        int existedCount = 0;
        List<AssignedTaskResponse> newTasks = new ArrayList<>();

        for (PmNotificationDto notif : pmNotifications) {
            if (!"TASK_ASSIGNED".equals(notif.type())) continue;
            if (notif.relatedTaskId() == null || notif.relatedTaskId().isBlank()) continue;

            if (assignedTaskRepository.existsByPmNotificationId(notif.notificationId())) {
                existedCount++;
                continue;
            }
            if (assignedTaskRepository.existsByPmTaskId(notif.relatedTaskId())) {
                existedCount++;
                continue;
            }

            AssignedTask last = assignedTaskRepository.findTopByOrderByIdDesc();
            String newId = IdGenerator.nextAssignedTaskId(last == null ? null : last.getId());

            AssignedTask task = AssignedTask.builder()
                    .id(newId)
                    .pmTaskId(notif.relatedTaskId())
                    .pmNotificationId(notif.notificationId())
                    .projectId(notif.projectId() != null ? notif.projectId() : "")
                    .assignedTo(currentUserId)
                    .assignedBy(notif.notificationFrom() != null ? notif.notificationFrom() : "")
                    .description(buildDescription(notif))
                    .status(AssignedTaskStatus.PENDING)
                    .syncedAt(LocalDateTime.now())
                    .build();

            assignedTaskRepository.save(task);
            newCount++;

            newTasks.add(toResponse(task));
        }

        log.info("Task sync for finance officer {}: {} new, {} already existed", currentUserId, newCount, existedCount);
        return new AssignedTaskSyncResult(newCount, existedCount, newTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskResponse> getMyTasks(String authorizationHeader) {
        String currentUserId = currentUserId(authorizationHeader);
        return assignedTaskRepository
                .findByAssignedToOrderBySyncedAtDesc(currentUserId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskResponse> getMyTasksByStatus(String authorizationHeader, AssignedTaskStatus status) {
        String currentUserId = currentUserId(authorizationHeader);
        return assignedTaskRepository
                .findByAssignedToAndStatusOrderBySyncedAtDesc(currentUserId, status)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskResponse> getMyTasksForProject(String authorizationHeader, String projectId) {
        String currentUserId = currentUserId(authorizationHeader);
        return assignedTaskRepository
                .findByAssignedToAndProjectId(currentUserId, projectId)
                .stream().map(this::toResponse).toList();
    }

    private String buildDescription(PmNotificationDto notif) {
        String base = notif.title() != null ? notif.title() : "";
        if (notif.message() != null && !notif.message().isBlank()) {
            base = notif.message();
        }
        return base.length() > 1000 ? base.substring(0, 1000) : base;
    }

    private AssignedTaskResponse toResponse(AssignedTask task) {
        return new AssignedTaskResponse(
                task.getId(),
                task.getPmTaskId(),
                task.getPmNotificationId(),
                task.getProjectId(),
                task.getAssignedTo(),
                task.getAssignedBy(),
                task.getDescription(),
                task.getStatus(),
                task.getLinkedEntityId(),
                task.getSyncedAt(),
                task.getCompletedAt()
        );
    }

    private String currentUserId(String authorizationHeader) {
        String token = jwtUtil.extractTokenFromHeader(authorizationHeader);
        String userId = jwtUtil.extractUserId(token);
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Could not resolve userId from JWT");
        }
        return userId;
    }
}
