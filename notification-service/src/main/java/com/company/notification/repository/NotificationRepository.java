package com.company.notification.repository;

import com.company.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RBAC enforcement happens IN the queries, not in service code.
 * That guarantees no caller can ever fetch another user's notifications
 * even if a service-layer bug forgets to filter.
 *
 * "departmentId IS NULL" handles role-wide broadcasts.
 */
@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long>,
                JpaSpecificationExecutor<Notification> {

    /* ---------------------- Bell icon (hot path) ---------------------- */

    @Query("""
           SELECT COUNT(n)
             FROM Notification n
            WHERE n.toRole = :role
              AND (n.toDepartmentId = :deptId OR n.toDepartmentId IS NULL)
              AND n.isRead = false
           """)
    long countUnreadForRecipient(@Param("role") String role,
                                 @Param("deptId") Long deptId);

    /* ---------------------- Listing (bell dropdown) ------------------- */

    @Query("""
           SELECT n FROM Notification n
            WHERE n.toRole = :role
              AND (n.toDepartmentId = :deptId OR n.toDepartmentId IS NULL)
            ORDER BY n.createdAt DESC
           """)
    Page<Notification> findAllForRecipient(@Param("role") String role,
                                           @Param("deptId") Long deptId,
                                           Pageable pageable);

    @Query("""
           SELECT n FROM Notification n
            WHERE n.toRole = :role
              AND (n.toDepartmentId = :deptId OR n.toDepartmentId IS NULL)
              AND (:eventType IS NULL OR n.eventType = :eventType)
              AND (:fromRole  IS NULL OR n.fromRole  = :fromRole)
            ORDER BY n.createdAt DESC
           """)
    Page<Notification> findAllForRecipientFiltered(@Param("role") String role,
                                                   @Param("deptId") Long deptId,
                                                   @Param("eventType") String eventType,
                                                   @Param("fromRole") String fromRole,
                                                   Pageable pageable);

    /* ---------------------- Mark as read (ownership-checked) ---------- */

    /**
     * Atomic, ownership-checked update. Returns the number of rows updated.
     * If 0 → either the notification doesn't exist OR it doesn't belong to
     * this recipient (RBAC violation). Either way we 404 — never 403,
     * to avoid leaking existence.
     */
    @Modifying
    @Query("""
           UPDATE Notification n
              SET n.isRead = true
            WHERE n.id = :id
              AND n.toRole = :role
              AND (n.toDepartmentId = :deptId OR n.toDepartmentId IS NULL)
              AND n.isRead = false
           """)
    int markAsReadIfOwned(@Param("id") Long id,
                          @Param("role") String role,
                          @Param("deptId") Long deptId);

    @Query("""
           SELECT n FROM Notification n
            WHERE n.id = :id
              AND n.toRole = :role
              AND (n.toDepartmentId = :deptId OR n.toDepartmentId IS NULL)
           """)
    Optional<Notification> findByIdForRecipient(@Param("id") Long id,
                                                @Param("role") String role,
                                                @Param("deptId") Long deptId);
}
