package com.buildsmart.vendor.service.impl;

import com.buildsmart.vendor.client.PmNotificationClient;
import com.buildsmart.vendor.client.dto.PmNotificationDto;
import com.buildsmart.vendor.dto.response.AssignedTaskResponse;
import com.buildsmart.vendor.dto.response.AssignedTaskSyncResult;
import com.buildsmart.vendor.entity.AssignedTask;
import com.buildsmart.vendor.enums.AssignedTaskStatus;
import com.buildsmart.vendor.repository.AssignedTaskRepository;
import com.buildsmart.vendor.security.AuthenticatedUserResolver;
import com.buildsmart.vendor.service.AssignedTaskService;
import com.buildsmart.vendor.util.IdGeneratorUtil;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Override
    public AssignedTaskSyncResult syncTasksFromPm(HttpServletRequest request) {
        String currentUserId = requireUserId(request);

        List<PmNotificationDto> pmNotifications;
        try {
            pmNotifications = pmNotificationClient.getNotificationsTo(currentUserId);
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
            String newId = IdGeneratorUtil.nextAssignedTaskId(last == null ? null : last.getId());

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

        log.info("Task sync for vendor {}: {} new, {} already existed", currentUserId, newCount, existedCount);
        return new AssignedTaskSyncResult(newCount, existedCount, newTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskResponse> getMyTasks(HttpServletRequest request) {
        String currentUserId = requireUserId(request);
        return assignedTaskRepository
                .findByAssignedToOrderBySyncedAtDesc(currentUserId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskResponse> getMyTasksByStatus(HttpServletRequest request, AssignedTaskStatus status) {
        String currentUserId = requireUserId(request);
        return assignedTaskRepository
                .findByAssignedToAndStatusOrderBySyncedAtDesc(currentUserId, status)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTaskResponse> getMyTasksForProject(HttpServletRequest request, String projectId) {
        String currentUserId = requireUserId(request);
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

    private String requireUserId(HttpServletRequest request) {
        String userId = authenticatedUserResolver.getCurrentUserId(request);
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Could not resolve authenticated vendor userId");
        }
        return userId;
    }
}
