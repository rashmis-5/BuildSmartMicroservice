package com.buildsmart.finance.service;

import com.buildsmart.finance.dto.request.ExpenseApprovalRequest;
import com.buildsmart.finance.dto.request.ExpenseCreateRequest;
import com.buildsmart.finance.dto.request.ExpenseUpdateRequest;
import com.buildsmart.finance.dto.response.ExpenseResponse;
import com.buildsmart.finance.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface ExpenseService {

    /**
     * Create a new expense
     */
    ExpenseResponse createExpense(ExpenseCreateRequest request);

    /**
     * Get expense by ID
     */
    ExpenseResponse getExpenseById(String expenseId);

    /**
     * Get all expenses for a budget with pagination
     */
    PagedResponse<ExpenseResponse> getExpensesByBudgetId(String budgetId, Pageable pageable);

    /**
     * Get all expenses for a project with pagination
     */
    PagedResponse<ExpenseResponse> getExpensesByProjectId(String projectId, Pageable pageable);

    /**
     * Submit expense for approval (DRAFT -> SUBMITTED)
     */
    ExpenseResponse submitExpenseForApproval(String expenseId);

    /**
     * Approve/Reject expense (called by Project Manager Service)
     */
    ExpenseResponse approveExpense(String expenseId, ExpenseApprovalRequest request);

    /**
     * Get expenses by status with pagination
     */
    PagedResponse<ExpenseResponse> getExpensesByStatus(String status, Pageable pageable);

    /**
     * Get expenses created by user
     */
    PagedResponse<ExpenseResponse> getExpensesByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Update expense (PATCH) - Only allowed for DRAFT status
     * Can only update description, amount, and expenseDate
     */
    ExpenseResponse updateExpense(String expenseId, ExpenseUpdateRequest request);

    /**
     * Delete expense - Only allowed for DRAFT status
     * Approved and Rejected expenses cannot be deleted
     */
    void deleteExpense(String expenseId);
}
