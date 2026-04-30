package com.buildsmart.vendor.controller;

import com.buildsmart.vendor.dto.response.AssignedTaskResponse;
import com.buildsmart.vendor.dto.response.AssignedTaskSyncResult;
import com.buildsmart.vendor.enums.AssignedTaskStatus;
import com.buildsmart.vendor.service.AssignedTaskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/tasks")
@RequiredArgsConstructor
public class AssignedTaskController {

    private final AssignedTaskService assignedTaskService;

    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public ResponseEntity<AssignedTaskSyncResult> sync(HttpServletRequest request) {
        return ResponseEntity.ok(assignedTaskService.syncTasksFromPm(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public ResponseEntity<List<AssignedTaskResponse>> getMyTasks(
            HttpServletRequest request,
            @RequestParam(required = false) AssignedTaskStatus status) {
        if (status != null) {
            return ResponseEntity.ok(assignedTaskService.getMyTasksByStatus(request, status));
        }
        return ResponseEntity.ok(assignedTaskService.getMyTasks(request));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public ResponseEntity<List<AssignedTaskResponse>> getMyTasksForProject(
            HttpServletRequest request,
            @PathVariable String projectId) {
        return ResponseEntity.ok(assignedTaskService.getMyTasksForProject(request, projectId));
    }
}
