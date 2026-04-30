package com.buildsmart.finance.service.impl;

import com.buildsmart.finance.dto.request.ExpenseApprovalRequest;
import com.buildsmart.finance.dto.request.ExpenseCreateRequest;
import com.buildsmart.finance.dto.request.ExpenseUpdateRequest;
import com.buildsmart.finance.dto.response.ExpenseResponse;
import com.buildsmart.finance.dto.response.PagedResponse;
import com.buildsmart.finance.entity.Budget;
import com.buildsmart.finance.entity.Expense;
import com.buildsmart.finance.entity.enums.ExpenseStatus;
import com.buildsmart.finance.exception.ResourceNotFoundException;
import com.buildsmart.finance.repository.BudgetRepository;
import com.buildsmart.finance.repository.ExpenseRepository;
import com.buildsmart.finance.service.ExpenseService;
import com.buildsmart.finance.util.IdGenerator;
import com.buildsmart.finance.util.JwtUtil;
import com.buildsmart.finance.validator.ExpenseValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseValidator expenseValidator;
    private final JwtUtil jwtUtil;

    @Override
    public ExpenseResponse createExpense(ExpenseCreateRequest request) {
        log.info("Creating expense for budget: {}", request.getBudgetId());

        // Validate budget exists and is approved
        Budget budget = budgetRepository.findById(request.getBudgetId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-002",
                        request.getBudgetId(),
                        "Budget not found with ID: " + request.getBudgetId()
                ));

        expenseValidator.validateExpenseCreation(request, budget);

        String expenseId = IdGenerator.generateExpenseId();
        
        // Auto-fill createdBy from JWT token
        String createdBy = extractUserIdFromToken();

        Expense expense = Expense.builder()
                .expenseId(expenseId)
                .projectId(request.getProjectId())
                .budgetId(request.getBudgetId())
                .expenseType(request.getExpenseType())
                .description(request.getDescription())
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate())
                .status(ExpenseStatus.DRAFT)
                .createdBy(createdBy)
                .isDeleted(false)
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        log.info("Expense created successfully with ID: {}", expenseId);

        publishExpenseCreatedEvent(savedExpense);

        return mapToResponse(savedExpense);
    }

    @Override
    public ExpenseResponse getExpenseById(String expenseId) {
        log.info("Fetching expense with ID: {}", expenseId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-003",
                        expenseId,
                        "Expense not found with ID: " + expenseId
                ));

        return mapToResponse(expense);
    }

    @Override
    public PagedResponse<ExpenseResponse> getExpensesByBudgetId(String budgetId, Pageable pageable) {
        log.info("Fetching expenses for budget: {}", budgetId);

        Page<Expense> expenses = expenseRepository.findByBudgetId(budgetId, pageable);
        return buildPagedResponse(expenses);
    }

    @Override
    public PagedResponse<ExpenseResponse> getExpensesByProjectId(String projectId, Pageable pageable) {
        log.info("Fetching expenses for project: {}", projectId);

        Page<Expense> expenses = expenseRepository.findByProjectId(projectId, pageable);
        return buildPagedResponse(expenses);
    }

    @Override
    public ExpenseResponse submitExpenseForApproval(String expenseId) {
        log.info("Submitting expense for approval: {}", expenseId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-003",
                        expenseId,
                        "Expense not found with ID: " + expenseId
                ));

        expenseValidator.validateExpenseCanBeSubmitted(expense.getStatus().name());

        expense.setStatus(ExpenseStatus.SUBMITTED);
        Expense updatedExpense = expenseRepository.save(expense);

        publishExpenseSubmittedEvent(updatedExpense);

        return mapToResponse(updatedExpense);
    }

    @Override
    public ExpenseResponse approveExpense(String expenseId, ExpenseApprovalRequest request) {
        log.info("Processing expense approval for ID: {} with status: {}", expenseId, request.getStatus());

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-003",
                        expenseId,
                        "Expense not found with ID: " + expenseId
                ));

        expense.setStatus(request.getStatus());
        expense.setApprovedBy(request.getApprovedBy());
        expense.setApprovedAt(LocalDateTime.now());

        if (request.getStatus() == ExpenseStatus.REJECTED) {
            expense.setRejectionReason(request.getRejectionReason());
        }

        Expense updatedExpense = expenseRepository.save(expense);

        if (request.getStatus() == ExpenseStatus.APPROVED) {
            publishExpenseApprovedEvent(updatedExpense);
        } else if (request.getStatus() == ExpenseStatus.REJECTED) {
            publishExpenseRejectedEvent(updatedExpense);
        }

        return mapToResponse(updatedExpense);
    }

    @Override
    public PagedResponse<ExpenseResponse> getExpensesByStatus(String status, Pageable pageable) {
        log.info("Fetching expenses with status: {}", status);

        ExpenseStatus expenseStatus = ExpenseStatus.valueOf(status.toUpperCase());
        Page<Expense> expenses = expenseRepository.findByStatus(expenseStatus, pageable);
        return buildPagedResponse(expenses);
    }

    @Override
    public PagedResponse<ExpenseResponse> getExpensesByCreatedBy(String createdBy, Pageable pageable) {
        log.info("Fetching expenses created by: {}", createdBy);

        Page<Expense> expenses = expenseRepository.findByCreatedBy(createdBy, pageable);
        return buildPagedResponse(expenses);
    }

    @Override
    public ExpenseResponse updateExpense(String expenseId, ExpenseUpdateRequest request) {
        log.info("Updating expense with ID: {}", expenseId);

        // Get expense by ID
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-003",
                        expenseId,
                        "Expense not found with ID: " + expenseId
                ));

        // Validate update request
        expenseValidator.validateExpenseUpdate(request);

        // Validate expense can be edited (only DRAFT status allowed)
        expenseValidator.validateExpenseCanBeEdited(expense);

        // Update only allowed fields
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate().atStartOfDay());

        Expense updatedExpense = expenseRepository.save(expense);
        log.info("Expense updated successfully with ID: {}", expenseId);

        return mapToResponse(updatedExpense);
    }

    @Override
    public void deleteExpense(String expenseId) {
        log.info("Deleting expense with ID: {}", expenseId);

        // Get expense by ID
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-003",
                        expenseId,
                        "Expense not found with ID: " + expenseId
                ));

        // Validate expense can be deleted (only DRAFT status allowed)
        expenseValidator.validateExpenseCanBeDeleted(expense);

        // Soft delete
        expense.setIsDeleted(true);
        expenseRepository.save(expense);

        log.info("Expense deleted successfully with ID: {}", expenseId);
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .expenseId(expense.getExpenseId())
                .projectId(expense.getProjectId())
                .budgetId(expense.getBudgetId())
                .expenseType(expense.getExpenseType())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .status(expense.getStatus())
                .createdBy(expense.getCreatedBy())
                .approvedBy(expense.getApprovedBy())
                .approvedAt(expense.getApprovedAt())
                .rejectionReason(expense.getRejectionReason())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    private PagedResponse<ExpenseResponse> buildPagedResponse(Page<Expense> page) {
        return PagedResponse.<ExpenseResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void publishExpenseCreatedEvent(Expense expense) {
        log.info("Publishing ExpenseCreatedEvent for expense: {}", expense.getExpenseId());
    }

    private void publishExpenseSubmittedEvent(Expense expense) {
        log.info("Publishing ExpenseSubmittedEvent for expense: {}", expense.getExpenseId());
    }

    private void publishExpenseApprovedEvent(Expense expense) {
        log.info("Publishing ExpenseApprovedEvent for expense: {}", expense.getExpenseId());
    }

    /**
     * Extract userId from JWT token stored in SecurityContext
     */
    private String extractUserIdFromToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object credentials = authentication.getCredentials();
                if (credentials != null) {
                    String token = credentials.toString();
                    String userId = jwtUtil.extractUserId(token);
                    if (userId != null && !userId.isEmpty()) {
                        log.debug("Extracted userId from JWT: {}", userId);
                        return userId;
                    }
                }
            }
            log.warn("Could not extract userId from token, using email instead");
            return extractUserFromSecurityContext();
        } catch (Exception e) {
            log.error("Error extracting userId from token: {}", e.getMessage());
            return extractUserFromSecurityContext();
        }
    }

    private String extractUserFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                return principal != null ? principal.toString() : "UNKNOWN";
            }
            return "UNKNOWN";
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    private void publishExpenseRejectedEvent(Expense expense) {
        log.info("Publishing ExpenseRejectedEvent for expense: {}", expense.getExpenseId());
    }
}
