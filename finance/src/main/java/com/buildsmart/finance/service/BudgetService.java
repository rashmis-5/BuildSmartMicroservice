package com.buildsmart.finance.service;

import com.buildsmart.finance.dto.request.BudgetApprovalRequest;
import com.buildsmart.finance.dto.request.BudgetCreateRequest;
import com.buildsmart.finance.dto.request.BudgetUpdateRequest;
import com.buildsmart.finance.dto.response.BudgetResponse;
import com.buildsmart.finance.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface BudgetService {

    /**
     * Create a new budget
     */
    BudgetResponse createBudget(BudgetCreateRequest request);

    /**
     * Get budget by ID
     */
    BudgetResponse getBudgetById(String budgetId);

    /**
     * Get all budgets for a project with pagination
     */
    PagedResponse<BudgetResponse> getBudgetsByProjectId(String projectId, Pageable pageable);

    /**
     * Submit budget for approval (DRAFT -> requires PM approval)
     */
    BudgetResponse submitBudgetForApproval(String budgetId);

    /**
     * Approve/Reject budget (called by Project Manager Service)
     */
    BudgetResponse approveBudget(String budgetId, BudgetApprovalRequest request);

    /**
     * Get budgets by status with pagination
     */
    PagedResponse<BudgetResponse> getBudgetsByStatus(String status, Pageable pageable);

    /**
     * Get budgets created by user
     */
    PagedResponse<BudgetResponse> getBudgetsByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Update budget (PATCH) - Only allowed for DRAFT status
     * Can only update plannedAmount and budgetCategory
     */
    BudgetResponse updateBudget(String budgetId, BudgetUpdateRequest request);

    /**
     * Delete budget - Only allowed for DRAFT status
     * Approved and Rejected budgets cannot be deleted
     */
    void deleteBudget(String budgetId);
}
