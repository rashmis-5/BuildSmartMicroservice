package com.company.notification.dto;

import com.company.notification.model.Notification;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String eventType;
    private String message;
    private String fromService;
    private String fromRole;
    private Long fromDepartmentId;
    private String toRole;
    private Long toDepartmentId;
    private String referenceId;
    private boolean read;
    private String payload;
    private Instant createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .eventType(n.getEventType())
                .message(n.getMessage())
                .fromService(n.getFromService())
                .fromRole(n.getFromRole())
                .fromDepartmentId(n.getFromDepartmentId())
                .toRole(n.getToRole())
                .toDepartmentId(n.getToDepartmentId())
                .referenceId(n.getReferenceId())
                .read(n.isRead())
                .payload(n.getPayload())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
