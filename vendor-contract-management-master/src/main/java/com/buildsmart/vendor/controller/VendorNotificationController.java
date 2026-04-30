package com.buildsmart.vendor.controller;

import com.buildsmart.vendor.client.IAMServiceClient;
import com.buildsmart.vendor.client.dto.UserDto;
import com.buildsmart.vendor.entity.VendorNotification;
import com.buildsmart.vendor.service.VendorNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor-notifications")
@Tag(name = "Vendor Notifications", description = "Endpoints for managing vendor notifications")
public class VendorNotificationController {

    private final VendorNotificationService notificationService;
    private final IAMServiceClient iamServiceClient;

    public VendorNotificationController(VendorNotificationService notificationService, IAMServiceClient iamServiceClient) {
        this.notificationService = notificationService;
        this.iamServiceClient = iamServiceClient;
    }

    @Operation(summary = "Get all notifications for the logged-in vendor")
    @GetMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<List<VendorNotification>> getMyNotifications(HttpServletRequest request) {
        String loggedInVendorId = getLoggedInVendorId(request);
        return ResponseEntity.ok(notificationService.getNotificationsByVendorId(loggedInVendorId));
    }

    @Operation(summary = "Get unread notification count for the logged-in vendor")
    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Long> getMyUnreadCount(HttpServletRequest request) {
        String loggedInVendorId = getLoggedInVendorId(request);
        return ResponseEntity.ok(notificationService.getUnreadCount(loggedInVendorId));
    }

    @Operation(summary = "Mark a notification as read")
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns the logged-in vendor's IAM userId (e.g. BSVM002).
     *
     * Notifications are persisted keyed by userId — both the PM-pushed
     * "TASK_ASSIGNED" entries (vendorId from task.assignedTo, which is the
     * IAM userId) and vendor-side notifications created during contract /
     * invoice / document flows. So we MUST look them up by userId here.
     *
     * Earlier this method returned user.name() (the display name like
     * "John Doe"), which caused notifications to never be returned because
     * the vendor_notification.vendor_id column stores the userId.
     */
    private String getLoggedInVendorId(HttpServletRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String token = request.getHeader("Authorization");
        UserDto user = iamServiceClient.getUserByEmail(email, token);
        return user.userId();
    }
}
