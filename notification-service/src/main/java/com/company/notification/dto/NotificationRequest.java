package com.company.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Inbound payload for POST /notifications.
 * Sent by other microservices via Feign.
 *
 * The producer is responsible for telling US who the recipient is
 * (toRole + optional toDepartmentId). This service does NOT do user lookup —
 * it stays generic.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class NotificationRequest {

    @NotBlank
    @Size(max = 64)
    private String eventType;

    @NotBlank
    @Size(max = 1000)
    private String message;

    @Size(max = 64)
    private String fromService;

    @Size(max = 64)
    private String fromRole;

    private Long fromDepartmentId;

    @NotBlank
    @Size(max = 64)
    private String toRole;

    /** Nullable: omit for role-wide broadcast. */
    private Long toDepartmentId;

    @Size(max = 128)
    private String referenceId;

    /** Optional JSON string. Service stores as-is. */
    private String payload;
}
