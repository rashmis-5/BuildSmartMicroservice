package com.company.notification.model;

import com.company.notification.enums.EventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Single notifications table. No JOINs. No business linkage.
 *
 * Indexes are critical because the bell icon polls every 15-30s. The
 * (toRole, toDepartmentId, isRead) composite index makes the unread-count
 * query O(log n) on a single index seek.
 *
 * referenceId is a generic foreign key (taskId, invoiceId, etc.) — kept as
 * a String so any producer can put any ID format here without coupling.
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                // Hot path: unread-count and listing for a given recipient
                @Index(name = "idx_recipient_unread",
                        columnList = "to_role, to_department_id, is_read"),
                // Listing notifications ordered by recency for a recipient
                @Index(name = "idx_recipient_created",
                        columnList = "to_role, to_department_id, created_at"),
                // Optional filter: by event type for a recipient
                @Index(name = "idx_recipient_event",
                        columnList = "to_role, to_department_id, event_type"),
                // Lookup by reference (e.g. show all notifications for invoice 42)
                @Index(name = "idx_reference", columnList = "reference_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stored as String (not @Enumerated) so a producer can send a new event
     * type without forcing a DB migration on consumers.
     */
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "from_service", length = 64)
    private String fromService;

    @Column(name = "from_role", length = 64)
    private String fromRole;

    @Column(name = "from_department_id")
    private Long fromDepartmentId;

    /**
     * Recipient role — REQUIRED. RBAC filter pivots on this.
     */
    @Column(name = "to_role", nullable = false, length = 64)
    private String toRole;

    /**
     * Recipient department — nullable for cross-department broadcasts to a role.
     * If null, every user with toRole sees it; otherwise only users in that dept.
     */
    @Column(name = "to_department_id")
    private Long toDepartmentId;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    /**
     * Optional JSON payload for the frontend (deep-link params, icons, etc.).
     * Stored as TEXT/JSON depending on the dialect.
     */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload")
    private String payload;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Convenience for marking read — keeps the field name change in one place.
     */
    public void markRead() {
        this.isRead = true;
    }

    public static EventType eventTypeOrGeneric(String raw) {
        try {
            return EventType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            return EventType.GENERIC;
        }
    }
}
