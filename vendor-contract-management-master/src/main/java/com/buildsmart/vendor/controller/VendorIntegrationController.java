package com.buildsmart.vendor.controller;

import com.buildsmart.vendor.service.DocumentService;
import com.buildsmart.vendor.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal integration controller called by the Project Manager module
 * to push approval/rejection decisions back into the Vendor module.
 *
 * Flow: PM approves/rejects → calls PUT /api/vendor-integration/approvals/{approvalId}/status
 *       → this controller updates invoice or document status accordingly
 *       → vendor notification is fired inside the service layer with PM's name
 */
@RestController
@RequestMapping("/api/vendor-integration")
public class VendorIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(VendorIntegrationController.class);

    private final InvoiceService invoiceService;
    private final DocumentService documentService;
    private final com.buildsmart.vendor.client.ProjectManagerClient projectManagerClient;
    private final com.buildsmart.vendor.service.VendorNotificationService vendorNotificationService;
    private final com.buildsmart.vendor.service.ApprovalSyncService approvalSyncService;

    public VendorIntegrationController(InvoiceService invoiceService,
                                       DocumentService documentService,
                                       com.buildsmart.vendor.client.ProjectManagerClient projectManagerClient,
                                       com.buildsmart.vendor.service.VendorNotificationService vendorNotificationService,
                                       com.buildsmart.vendor.service.ApprovalSyncService approvalSyncService) {
        this.invoiceService = invoiceService;
        this.documentService = documentService;
        this.projectManagerClient = projectManagerClient;
        this.vendorNotificationService = vendorNotificationService;
        this.approvalSyncService = approvalSyncService;
    }

    /**
     * Pull every approval decision from PM and reconcile vendor state.
     *
     * <p>The Project Manager module currently does not push approve/reject
     * decisions back into the vendor module, so the vendor's invoices and
     * documents would otherwise stay {@code SUBMITTED} forever. Calling this
     * endpoint after PM acts (or on a schedule) updates vendor statuses and
     * fires the corresponding APPROVAL_ACCEPTED / APPROVAL_REJECTED
     * notifications to the vendor.</p>
     */
    @PostMapping("/sync-from-pm")
    public ResponseEntity<com.buildsmart.vendor.service.ApprovalSyncService.SyncReport> syncFromPM(
            jakarta.servlet.http.HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return ResponseEntity.ok(approvalSyncService.syncAll(authorization));
    }

    /**
     * Called by PM to notify vendor of approval/rejection decision.
     * Tries to match approvalId against invoices first, then documents.
     *
     * @param approvalId       the approval ID (APRVN-xxx format)
     * @param status           "APPROVED" or "REJECTED"
     * @param actionBy         the PM user ID who took the action
     * @param approvedByName   the PM user's display name (shown in vendor notification)
     * @param rejectionReason  mandatory when status is REJECTED; null when APPROVED
     */
    @PutMapping("/approvals/{approvalId}/status")
    public ResponseEntity<String> updateApprovalStatus(
            @PathVariable("approvalId") String approvalId,
            @RequestParam("status") String status,
            @RequestParam("rejectedBy") String actionBy,
            @RequestParam(value = "approvedByName", required = false) String approvedByName,
            @RequestParam(value = "rejectionReason", required = false) String rejectionReason) {

        if (approvalId == null || approvalId.isBlank()) {
            return ResponseEntity.badRequest().body("approvalId is required");
        }
        com.buildsmart.vendor.validator.IdFormatValidator.requireValid(
                com.buildsmart.vendor.validator.IdFormatValidator.Kind.APPROVAL, approvalId);

        // Use approvedByName in messages; fall back to actionBy if not provided
        String pmDisplayName = (approvedByName != null && !approvedByName.isBlank()) ? approvedByName : actionBy;

        log.info("Received approval status update from PM: approvalId={}, status={}, actionBy={}, pmName={}",
                approvalId, status, actionBy, pmDisplayName);

        boolean handled = false;

        if ("APPROVED".equalsIgnoreCase(status)) {
            try {
                invoiceService.updateInvoiceApprovalStatusByApprovalId(approvalId, pmDisplayName);
                log.info("Invoice approved for approvalId={}, approvedByName={}", approvalId, pmDisplayName);
                handled = true;
            } catch (RuntimeException invoiceEx) {
                log.debug("No invoice found for approvalId={}, trying documents. Reason: {}", approvalId, invoiceEx.getMessage());
                try {
                    documentService.updateDocumentApprovalStatusByApprovalId(approvalId, pmDisplayName);
                    log.info("Document approved for approvalId={}, approvedByName={}", approvalId, pmDisplayName);
                    handled = true;
                } catch (RuntimeException docEx) {
                    log.warn("No document found for approvalId={} either. Reason: {}", approvalId, docEx.getMessage());
                }
            }

        } else if ("REJECTED".equalsIgnoreCase(status)) {
            try {
                invoiceService.updateInvoiceRejectionStatusByApprovalId(approvalId, pmDisplayName, rejectionReason);
                log.info("Invoice rejected for approvalId={}, rejectedByName={}", approvalId, pmDisplayName);
                handled = true;
            } catch (RuntimeException invoiceEx) {
                log.debug("No invoice found for approvalId={}, trying documents. Reason: {}", approvalId, invoiceEx.getMessage());
                try {
                    documentService.updateDocumentRejectionStatusByApprovalId(approvalId, pmDisplayName, rejectionReason);
                    log.info("Document rejected for approvalId={}, rejectedByName={}", approvalId, pmDisplayName);
                    handled = true;
                } catch (RuntimeException docEx) {
                    log.warn("No document found for approvalId={} either. Reason: {}", approvalId, docEx.getMessage());
                }
            }

        } else {
            return ResponseEntity.badRequest().body("Invalid status: " + status + ". Must be APPROVED or REJECTED.");
        }

        if (!handled) {
            log.error("No invoice or document found for approvalId={}", approvalId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok("Approval status updated successfully");
    }

    /**
     * Proxy endpoint: Get tasks for a project from PM (used by vendor UI if needed).
     */
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<java.util.List<Object>> getTasksForProject(@PathVariable("projectId") String projectId) {
        com.buildsmart.vendor.validator.IdFormatValidator.requireValid(
                com.buildsmart.vendor.validator.IdFormatValidator.Kind.PROJECT, projectId);
        return ResponseEntity.ok(projectManagerClient.getProjectTasks(projectId));
    }

    /**
     * Called by PM whenever a task is created/assigned to a vendor.
     * Saves a notification in the vendor's feed so the vendor immediately sees
     * the new task with full context (taskId, projectId, description, dates).
     *
     * PM endpoint that calls this: POST /api/vendor-integration/tasks/notify
     */
    @PostMapping("/tasks/notify")
    public ResponseEntity<String> notifyTaskAssigned(
            @RequestParam("vendorId") String vendorId,
            @RequestParam("taskId") String taskId,
            @RequestParam("projectId") String projectId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "plannedStart", required = false) String plannedStart,
            @RequestParam(value = "plannedEnd", required = false) String plannedEnd,
            @RequestParam(value = "assignedDepartment", required = false) String assignedDepartment) {

        if (vendorId == null || vendorId.isBlank()) {
            return ResponseEntity.badRequest().body("vendorId is required");
        }
        if (taskId == null || taskId.isBlank()) {
            return ResponseEntity.badRequest().body("taskId is required");
        }
        if (projectId == null || projectId.isBlank()) {
            return ResponseEntity.badRequest().body("projectId is required");
        }
        com.buildsmart.vendor.validator.IdFormatValidator.requireValid(
                com.buildsmart.vendor.validator.IdFormatValidator.Kind.VENDOR, vendorId);
        com.buildsmart.vendor.validator.IdFormatValidator.requireValid(
                com.buildsmart.vendor.validator.IdFormatValidator.Kind.TASK, taskId);
        com.buildsmart.vendor.validator.IdFormatValidator.requireValid(
                com.buildsmart.vendor.validator.IdFormatValidator.Kind.PROJECT, projectId);

        String title = String.format("New Task Assigned: %s", taskId);
        String message = String.format(
                "You have been assigned a new task (%s): %s. Planned: %s to %s", 
                taskId, 
                description != null ? description : "N/A", 
                plannedStart != null ? plannedStart : "N/A", 
                plannedEnd != null ? plannedEnd : "N/A"
        );

        try {
            vendorNotificationService.createNotification(
                vendorId,
                projectId,
                taskId,
                title,
                message,
                com.buildsmart.vendor.enums.VendorNotificationType.TASK_ASSIGNED
            );
            log.info("Vendor notified of task assignment: vendorId={}, taskId={}, projectId={}",
                vendorId, taskId, projectId);
            // Notify Project Manager module as well
            projectManagerClient.notifyTaskAssigned(
                vendorId, taskId, projectId, description, plannedStart, plannedEnd, assignedDepartment
            );
            log.info("PM notified of task assignment: vendorId={}, taskId={}, projectId={}",
                vendorId, taskId, projectId);
            return ResponseEntity.ok("Task assignment notification stored for vendor " + vendorId);
        } catch (Exception e) {
            log.error("Failed to store task assignment notification for vendorId={}, taskId={}: {}",
                vendorId, taskId, e.getMessage());
            return ResponseEntity.status(500).body("Failed to store notification: " + e.getMessage());
        }
    }
}
