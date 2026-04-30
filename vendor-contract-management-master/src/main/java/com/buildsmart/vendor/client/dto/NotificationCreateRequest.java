package com.buildsmart.vendor.client.dto;

/**
 * Outbound payload for POST /api/notifications on the Notification service.
 * Mirrors com.company.notification.dto.NotificationRequest.
 */
public record NotificationCreateRequest(
        String eventType,
        String message,
        String fromService,
        String fromRole,
        Long fromDepartmentId,
        String toRole,
        Long toDepartmentId,
        String referenceId,
        String payload
) {}
