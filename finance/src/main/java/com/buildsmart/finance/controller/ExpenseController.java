package com.buildsmart.finance.controller;

import com.buildsmart.finance.dto.request.ExpenseApprovalRequest;
import com.buildsmart.finance.dto.request.ExpenseCreateRequest;
import com.buildsmart.finance.dto.request.ExpenseUpdateRequest;
import com.buildsmart.finance.dto.response.ExpenseResponse;
import com.buildsmart.finance.dto.response.PagedResponse;
import com.buildsmart.finance.service.ExpenseService;
import com.buildsmart.finance.util.PaginationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * Create a new expense
     * POST /api/expenses
     */
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseCreateRequest request) {
        log.info("POST /api/expenses - Creating expense");
        ExpenseResponse response = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get expense by ID
     * GET /api/expenses/{expenseId}
     */
    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable String expenseId) {
        log.info("GET /api/expenses/{} - Fetching expense", expenseId);
        ExpenseResponse response = expenseService.getExpenseById(expenseId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get expenses for a budget with pagination
     * GET /api/expenses/budgets/{budgetId}?page=0&size=10
     */
    @GetMapping("/budgets/{budgetId}")
    public ResponseEntity<PagedResponse<ExpenseResponse>> getExpensesByBudget(
            @PathVariable String budgetId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/expenses/budgets/{} - Fetching expenses", budgetId);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<ExpenseResponse> response = expenseService.getExpensesByBudgetId(budgetId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get expenses for a project with pagination
     * GET /api/expenses/projects/{projectId}?page=0&size=10
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<PagedResponse<ExpenseResponse>> getExpensesByProject(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/expenses/projects/{} - Fetching expenses", projectId);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<ExpenseResponse> response = expenseService.getExpensesByProjectId(projectId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get expense revisions
     * GET /api/expenses/{expenseId}/revisions?page=0&size=10
     */
    // @GetMapping("/{expenseId}/revisions")
    // public ResponseEntity<PagedResponse<ExpenseResponse>> getExpenseRevisions(
    //         @PathVariable String expenseId,
    //         @RequestParam(defaultValue = "0") Integer page,
    //         @RequestParam(defaultValue = "10") Integer size,
    //         @RequestParam(defaultValue = "createdAt") String sortBy,
    //         @RequestParam(defaultValue = "DESC") String sortOrder) {
    //     log.info("GET /api/expenses/{}/revisions - Fetching revisions", expenseId);

    //     Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
    //     PagedResponse<ExpenseResponse> response = expenseService.getExpenseRevisions(expenseId, pageable);
    //     return ResponseEntity.ok(response);
    // }

    /**
     * Submit expense for approval
     * POST /api/expenses/{expenseId}/submit
     */
    @PostMapping("/{expenseId}/submit")
    public ResponseEntity<ExpenseResponse> submitExpenseForApproval(@PathVariable String expenseId) {
        log.info("POST /api/expenses/{}/submit - Submitting expense", expenseId);
        ExpenseResponse response = expenseService.submitExpenseForApproval(expenseId);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve/Reject/Request Revision for expense
     * POST /api/expenses/{expenseId}/approval
     */
    @PostMapping("/{expenseId}/approval")
    public ResponseEntity<ExpenseResponse> approveExpense(
            @PathVariable String expenseId,
            @Valid @RequestBody ExpenseApprovalRequest request) {
        log.info("POST /api/expenses/{}/approval - Processing approval", expenseId);
        ExpenseResponse response = expenseService.approveExpense(expenseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Create expense revision (when rejected)
     * POST /api/expenses/{parentExpenseId}/revisions
     */
    // @PostMapping("/{parentExpenseId}/revisions")
    // public ResponseEntity<ExpenseResponse> createExpenseRevision(
    //         @PathVariable String parentExpenseId,
    //         @Valid @RequestBody ExpenseCreateRequest request) {
    //     log.info("POST /api/expenses/{}/revisions - Creating revision", parentExpenseId);
    //     ExpenseResponse response = expenseService.createExpenseRevision(parentExpenseId, request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(response);
    // }

    /**
     * Get expenses by status with pagination
     * GET /api/expenses/status/{status}?page=0&size=10
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<ExpenseResponse>> getExpensesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/expenses/status/{} - Fetching expenses", status);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<ExpenseResponse> response = expenseService.getExpensesByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get expenses by creator
     * GET /api/expenses/users/{createdBy}?page=0&size=10
     */
    @GetMapping("/users/{createdBy}")
    public ResponseEntity<PagedResponse<ExpenseResponse>> getExpensesByCreatedBy(
            @PathVariable String createdBy,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/expenses/users/{} - Fetching expenses", createdBy);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<ExpenseResponse> response = expenseService.getExpensesByCreatedBy(createdBy, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Update expense (PATCH)
     * PATCH /api/expenses/{expenseId}
     * Only allowed for DRAFT status expenses
     * Can only update description, amount, and expenseDate
     */
    @PatchMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable String expenseId,
            @Valid @RequestBody ExpenseUpdateRequest request) {
        log.info("PATCH /api/expenses/{} - Updating expense", expenseId);
        ExpenseResponse response = expenseService.updateExpense(expenseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete expense
     * DELETE /api/expenses/{expenseId}
     * Only allowed for DRAFT status expenses
     * Approved and Rejected expenses cannot be deleted
     */
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable String expenseId) {
        log.info("DELETE /api/expenses/{} - Deleting expense", expenseId);
        expenseService.deleteExpense(expenseId);
        return ResponseEntity.noContent().build();
    }
}
