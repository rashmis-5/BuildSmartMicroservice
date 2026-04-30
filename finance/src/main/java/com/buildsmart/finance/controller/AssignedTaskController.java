package com.buildsmart.finance.controller;

import com.buildsmart.finance.dto.response.AssignedTaskResponse;
import com.buildsmart.finance.dto.response.AssignedTaskSyncResult;
import com.buildsmart.finance.entity.enums.AssignedTaskStatus;
import com.buildsmart.finance.service.AssignedTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/tasks")
@RequiredArgsConstructor
public class AssignedTaskController {

    private final AssignedTaskService assignedTaskService;

    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER','ADMIN')")
    public ResponseEntity<AssignedTaskSyncResult> sync(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(assignedTaskService.syncTasksFromPm(authorization));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER','ADMIN')")
    public ResponseEntity<List<AssignedTaskResponse>> getMyTasks(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) AssignedTaskStatus status) {
        if (status != null) {
            return ResponseEntity.ok(assignedTaskService.getMyTasksByStatus(authorization, status));
        }
        return ResponseEntity.ok(assignedTaskService.getMyTasks(authorization));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER','ADMIN')")
    public ResponseEntity<List<AssignedTaskResponse>> getMyTasksForProject(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String projectId) {
        return ResponseEntity.ok(assignedTaskService.getMyTasksForProject(authorization, projectId));
    }
}
