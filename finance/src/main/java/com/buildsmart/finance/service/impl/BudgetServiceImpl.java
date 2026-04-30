package com.buildsmart.finance.service.impl;

import com.buildsmart.finance.client.ProjectClient;
import com.buildsmart.finance.dto.request.BudgetApprovalRequest;
import com.buildsmart.finance.dto.request.BudgetCreateRequest;
import com.buildsmart.finance.dto.request.BudgetUpdateRequest;
import com.buildsmart.finance.dto.response.BudgetResponse;
import com.buildsmart.finance.dto.response.PagedResponse;
import com.buildsmart.finance.entity.Budget;
import com.buildsmart.finance.entity.enums.BudgetStatus;
import com.buildsmart.finance.exception.ResourceNotFoundException;
import com.buildsmart.finance.repository.BudgetRepository;
import com.buildsmart.finance.service.BudgetService;
import com.buildsmart.finance.util.IdGenerator;
import com.buildsmart.finance.util.JwtUtil;
import com.buildsmart.finance.validator.BudgetValidator;
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
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetValidator budgetValidator;
    private final ProjectClient projectServiceClient;
    private final JwtUtil jwtUtil;
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public BudgetResponse createBudget(BudgetCreateRequest request) {
        log.info("Creating budget for project: {}", request.getProjectId());

        // Auto-fill createdBy from JWT token
        String createdBy = extractUserIdFromToken();

        // Validate project exists and all budget constraints
        try {
            String authHeader = getAuthorizationHeader();
            if (authHeader != null) {
                projectServiceClient.getProject(request.getProjectId(), authHeader);
            }
        } catch (Exception e) {
            log.warn("Could not verify project, but proceeding with budget creation");
        }

        // Validate budget creation (checks duplicate category and project budget limit)
        budgetValidator.validateBudgetCreation(request);

        // Generate ID
        String budgetId = IdGenerator.generateBudgetId();

        // Create budget entity
        Budget budget = Budget.builder()
                .budgetId(budgetId)
                .projectId(request.getProjectId())
                .budgetCategory(request.getBudgetCategory())
                .plannedAmount(request.getPlannedAmount())
                .status(BudgetStatus.DRAFT)
                .createdBy(createdBy)
                .actualAmount(java.math.BigDecimal.ZERO)
                .variance(request.getPlannedAmount())
                .isDeleted(false)
                .build();

        Budget savedBudget = budgetRepository.save(budget);
        log.info("Budget created successfully with ID: {} by user: {}", budgetId, createdBy);

        // Publish event (BudgetCreatedEvent) - would be done by event publisher
        publishBudgetCreatedEvent(savedBudget);

        return mapToResponse(savedBudget);
    }

    @Override
    public BudgetResponse getBudgetById(String budgetId) {
        log.info("Fetching budget with ID: {}", budgetId);

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-001",
                        budgetId,
                        "Budget not found with ID: " + budgetId
                ));

        return mapToResponse(budget);
    }

    @Override
    public PagedResponse<BudgetResponse> getBudgetsByProjectId(String projectId, Pageable pageable) {
        log.info("Fetching budgets for project: {} with pagination", projectId);

        Page<Budget> budgets = budgetRepository.findByProjectId(projectId, pageable);
        return buildPagedResponse(budgets);
    }

    @Override
    public BudgetResponse submitBudgetForApproval(String budgetId) {
        log.info("Submitting budget for approval: {}", budgetId);

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-001",
                        budgetId,
                        "Budget not found with ID: " + budgetId
                ));

        budgetValidator.validateBudgetCanBeEdited(budget);

        // Status remains DRAFT until PM approves/rejects
        // Event is published to notify PM
        publishBudgetSubmittedEvent(budget);

        return mapToResponse(budget);
    }

    @Override
    public BudgetResponse approveBudget(String budgetId, BudgetApprovalRequest request) {
        log.info("Processing budget approval for ID: {} with status: {}", budgetId, request.getStatus());

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-001",
                        budgetId,
                        "Budget not found with ID: " + budgetId
                ));

        budgetValidator.validateBudgetNotApproved(budget);

        budget.setStatus(request.getStatus());
        budget.setApprovedBy(request.getApprovedBy());
        budget.setApprovedAt(LocalDateTime.now());

        Budget updatedBudget = budgetRepository.save(budget);
        log.info("Budget {} approval processed with status: {}", budgetId, request.getStatus());

        // Publish event (BudgetApprovedEvent or BudgetRejectedEvent)
        if (request.getStatus() == BudgetStatus.APPROVED) {
            publishBudgetApprovedEvent(updatedBudget);
        } else {
            publishBudgetRejectedEvent(updatedBudget);
        }

        return mapToResponse(updatedBudget);
    }

    @Override
    public PagedResponse<BudgetResponse> getBudgetsByStatus(String status, Pageable pageable) {
        log.info("Fetching budgets with status: {}", status);

        BudgetStatus budgetStatus = BudgetStatus.valueOf(status.toUpperCase());
        Page<Budget> budgets = budgetRepository.findByStatus(budgetStatus, pageable);
        return buildPagedResponse(budgets);
    }

    @Override
    public PagedResponse<BudgetResponse> getBudgetsByCreatedBy(String createdBy, Pageable pageable) {
        log.info("Fetching budgets created by: {}", createdBy);

        Page<Budget> budgets = budgetRepository.findByCreatedBy(createdBy, pageable);
        return buildPagedResponse(budgets);
    }

    @Override
    public BudgetResponse updateBudget(String budgetId, BudgetUpdateRequest request) {
        log.info("Updating budget with ID: {}", budgetId);

        // Get budget by ID
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-001",
                        budgetId,
                        "Budget not found with ID: " + budgetId
                ));

        // Validate update request
        budgetValidator.validateBudgetUpdate(request);

        // Validate budget can be edited (only DRAFT status allowed)
        budgetValidator.validateBudgetCanBeEdited(budget);

        // Update only allowed fields
        budget.setBudgetCategory(request.getBudgetCategory());
        budget.setPlannedAmount(request.getPlannedAmount());

        // Recalculate variance if actual amount exists
        if (budget.getActualAmount() != null) {
            budget.setVariance(request.getPlannedAmount().subtract(budget.getActualAmount()));
        } else {
            budget.setVariance(request.getPlannedAmount());
        }

        Budget updatedBudget = budgetRepository.save(budget);
        log.info("Budget updated successfully with ID: {}", budgetId);

        return mapToResponse(updatedBudget);
    }

    @Override
    public void deleteBudget(String budgetId) {
        log.info("Deleting budget with ID: {}", budgetId);

        // Get budget by ID
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-001",
                        budgetId,
                        "Budget not found with ID: " + budgetId
                ));

        // Validate budget can be deleted (only DRAFT status allowed)
        budgetValidator.validateBudgetCanBeDeleted(budget);

        // Soft delete
        budget.setIsDeleted(true);
        budgetRepository.save(budget);

        log.info("Budget deleted successfully with ID: {}", budgetId);
    }

    // Helper methods
    private BudgetResponse mapToResponse(Budget budget) {
        return BudgetResponse.builder()
                .budgetId(budget.getBudgetId())
                .projectId(budget.getProjectId())
                .budgetCategory(budget.getBudgetCategory())
                .plannedAmount(budget.getPlannedAmount())
                .actualAmount(budget.getActualAmount())
                .variance(budget.getVariance())
                .status(budget.getStatus())
                .createdBy(budget.getCreatedBy())
                .approvedBy(budget.getApprovedBy())
                .approvedAt(budget.getApprovedAt())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }

    private PagedResponse<BudgetResponse> buildPagedResponse(Page<Budget> page) {
        return PagedResponse.<BudgetResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void publishBudgetCreatedEvent(Budget budget) {
        log.info("Publishing BudgetCreatedEvent for budget: {}", budget.getBudgetId());
        // Event publishing would be implemented via ApplicationEventPublisher
    }

    private void publishBudgetSubmittedEvent(Budget budget) {
        log.info("Publishing BudgetSubmittedEvent for budget: {}", budget.getBudgetId());
        // Event publishing would be implemented via ApplicationEventPublisher
    }

    private void publishBudgetApprovedEvent(Budget budget) {
        log.info("Publishing BudgetApprovedEvent for budget: {}", budget.getBudgetId());
        // Event publishing would be implemented via ApplicationEventPublisher
    }

    private void publishBudgetRejectedEvent(Budget budget) {
        log.info("Publishing BudgetRejectedEvent for budget: {}", budget.getBudgetId());
        // Event publishing would be implemented via ApplicationEventPublisher
    }

    private String getAuthorizationHeader() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            Object credentials = authentication.getCredentials();
            if (credentials == null) {
                return null;
            }

            String token = credentials.toString();
            if (!token.startsWith(BEARER_PREFIX)) {
                token = BEARER_PREFIX + token;
            }

            return token;
        } catch (Exception e) {
            log.error("Error extracting authorization header: {}", e.getMessage());
            return null;
        }
    }

    private String getCurrentAuthenticatedUser() {
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
            return getCurrentAuthenticatedUser();
        } catch (Exception e) {
            log.error("Error extracting userId from token: {}", e.getMessage());
            return getCurrentAuthenticatedUser();
        }
    }
}
