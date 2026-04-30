package com.buildsmart.finance.controller;

import com.buildsmart.finance.dto.request.BudgetApprovalRequest;
import com.buildsmart.finance.dto.request.BudgetCreateRequest;
import com.buildsmart.finance.dto.request.BudgetUpdateRequest;
import com.buildsmart.finance.dto.response.BudgetResponse;
import com.buildsmart.finance.dto.response.PagedResponse;
import com.buildsmart.finance.service.BudgetService;
import com.buildsmart.finance.util.PaginationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER')")
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * Create a new budget
     * POST /api/budgets
     * Required role: ADMIN or FINANCE_OFFICER
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody BudgetCreateRequest request) {
        log.info("POST /api/budgets - Creating budget for project: {}", request.getProjectId());
        BudgetResponse response = budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get budget by ID
     * GET /api/budgets/{budgetId}
     * Required role: ADMIN or FINANCE_OFFICER
     */
    @GetMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> getBudget(@PathVariable String budgetId) {
        log.info("GET /api/budgets/{} - Fetching budget", budgetId);
        BudgetResponse response = budgetService.getBudgetById(budgetId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get budgets for a project with pagination
     * GET /api/budgets/projects/{projectId}?page=0&size=10&sortBy=createdAt&sortOrder=DESC
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<PagedResponse<BudgetResponse>> getBudgetsByProject(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/budgets/projects/{} - Fetching budgets", projectId);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<BudgetResponse> response = budgetService.getBudgetsByProjectId(projectId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get budget revisions
     * GET /api/budgets/{budgetId}/revisions?page=0&size=10
     */
    // @GetMapping("/{budgetId}/revisions")
    // public ResponseEntity<PagedResponse<BudgetResponse>> getBudgetRevisions(
    //         @PathVariable String budgetId,
    //         @RequestParam(defaultValue = "0") Integer page,
    //         @RequestParam(defaultValue = "10") Integer size,
    //         @RequestParam(defaultValue = "createdAt") String sortBy,
    //         @RequestParam(defaultValue = "DESC") String sortOrder) {
    //     log.info("GET /api/budgets/{}/revisions - Fetching revisions", budgetId);

    //     Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
    //     PagedResponse<BudgetResponse> response = budgetService.getBudgetRevisions(budgetId, pageable);
    //     return ResponseEntity.ok(response);
    // }

    /**
     * Submit budget for approval
     * POST /api/budgets/{budgetId}/submit
     */
    @PostMapping("/{budgetId}/submit")
    public ResponseEntity<BudgetResponse> submitBudgetForApproval(@PathVariable String budgetId) {
        log.info("POST /api/budgets/{}/submit - Submitting budget", budgetId);
        BudgetResponse response = budgetService.submitBudgetForApproval(budgetId);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve/Reject budget (called by PM Service)
     * POST /api/budgets/{budgetId}/approval
     */
    @PostMapping("/{budgetId}/approval")
    public ResponseEntity<BudgetResponse> approveBudget(
            @PathVariable String budgetId,
            @Valid @RequestBody BudgetApprovalRequest request) {
        log.info("POST /api/budgets/{}/approval - Processing approval", budgetId);
        BudgetResponse response = budgetService.approveBudget(budgetId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Create budget revision (when rejected)
     * POST /api/budgets/{parentBudgetId}/revisions
     */
    // @PostMapping("/{parentBudgetId}/revisions")
    // public ResponseEntity<BudgetResponse> createBudgetRevision(
    //         @PathVariable String parentBudgetId,
    //         @Valid @RequestBody BudgetCreateRequest request) {
    //     log.info("POST /api/budgets/{}/revisions - Creating revision", parentBudgetId);
    //     BudgetResponse response = budgetService.createBudgetRevision(parentBudgetId, request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(response);
    // }

    /**
     * Get budgets by status with pagination
     * GET /api/budgets/status/{status}?page=0&size=10
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<BudgetResponse>> getBudgetsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/budgets/status/{} - Fetching budgets", status);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<BudgetResponse> response = budgetService.getBudgetsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get budgets by creator with pagination
     * GET /api/budgets/users/{createdBy}?page=0&size=10
     */
    @GetMapping("/users/{createdBy}")
    public ResponseEntity<PagedResponse<BudgetResponse>> getBudgetsByCreatedBy(
            @PathVariable String createdBy,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/budgets/users/{} - Fetching budgets", createdBy);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<BudgetResponse> response = budgetService.getBudgetsByCreatedBy(createdBy, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Update budget (PATCH)
     * PATCH /api/budgets/{budgetId}
     * Only allowed for DRAFT status budgets
     * Can only update plannedAmount and budgetCategory
     */
    @PatchMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable String budgetId,
            @Valid @RequestBody BudgetUpdateRequest request) {
        log.info("PATCH /api/budgets/{} - Updating budget", budgetId);
        BudgetResponse response = budgetService.updateBudget(budgetId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete budget
     * DELETE /api/budgets/{budgetId}
     * Only allowed for DRAFT status budgets
     * Approved and Rejected budgets cannot be deleted
     */
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(@PathVariable String budgetId) {
        log.info("DELETE /api/budgets/{} - Deleting budget", budgetId);
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.noContent().build();
    }
}
