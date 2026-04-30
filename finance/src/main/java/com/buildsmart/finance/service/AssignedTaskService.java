package com.buildsmart.finance.service;

import com.buildsmart.finance.dto.response.AssignedTaskResponse;
import com.buildsmart.finance.dto.response.AssignedTaskSyncResult;
import com.buildsmart.finance.entity.enums.AssignedTaskStatus;

import java.util.List;

public interface AssignedTaskService {

    /**
     * Pulls TASK_ASSIGNED notifications from the project-service for the
     * currently authenticated finance officer and stores any new ones as
     * AssignedTask records.
     */
    AssignedTaskSyncResult syncTasksFromPm(String authorizationHeader);

    List<AssignedTaskResponse> getMyTasks(String authorizationHeader);

    List<AssignedTaskResponse> getMyTasksByStatus(String authorizationHeader, AssignedTaskStatus status);

    List<AssignedTaskResponse> getMyTasksForProject(String authorizationHeader, String projectId);
}
