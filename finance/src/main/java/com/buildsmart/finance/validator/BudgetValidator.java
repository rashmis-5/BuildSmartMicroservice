package com.buildsmart.finance.validator;

import java.math.BigDecimal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.buildsmart.finance.client.ProjectClient;
import com.buildsmart.finance.client.dto.ProjectDto;
import com.buildsmart.finance.dto.request.BudgetCreateRequest;
import com.buildsmart.finance.dto.request.BudgetUpdateRequest;
import com.buildsmart.finance.entity.Budget;
import com.buildsmart.finance.exception.BusinessRuleException;
import com.buildsmart.finance.exception.ValidationException;
import com.buildsmart.finance.repository.BudgetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetValidator {

    private final BudgetRepository budgetRepository;
    private final ProjectClient projectServiceClient;
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Validate budget creation request
     */
    public void validateBudgetCreation(BudgetCreateRequest request) {
        log.info("Validating budget creation request for project: {}", request.getProjectId());

        // Validate project exists
        validateProjectExists(request.getProjectId());

        // Validate planned amount
        if (request.getPlannedAmount() == null || request.getPlannedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "FIN-VAL-003",
                    "plannedAmount",
                    "Planned amount must be greater than 0"
            );
        }

        // Check for duplicate budget (same project + category)
        boolean exists = budgetRepository.existsByProjectIdAndCategory(
                request.getProjectId(),
                request.getBudgetCategory()
        );

        if (exists) {
            throw new BusinessRuleException(
                    "FIN-BUS-002",
                    "A budget already exists for project " + request.getProjectId() +
                            " with category " + request.getBudgetCategory().getDisplayName()
            );
        }

        // Validate budget amount doesn't exceed project budget
       validateBudgetAmountNotExceedingProjectBudget(request.getProjectId(), request.getPlannedAmount());
    }

    /**
     * Validate that project exists in Project Manager service
     */
    private void validateProjectExists(String projectId) {
        try {
            String authHeader = getAuthorizationHeader();
            if (authHeader == null) {
                log.warn("No authorization header for project validation");
                throw new BusinessRuleException(
                        "AUTH-ERROR",
                        "Authentication required to validate project"
                );
            }

            log.info("Validating project existence: {}", projectId);
            ProjectDto project = projectServiceClient.getProject(projectId, authHeader);

            if (project == null) {
                throw new BusinessRuleException(
                        "PROJECT-NOT-FOUND",
                        "Project with ID " + projectId + " does not exist"
                );
            }

            log.info("Project {} validated successfully", projectId);

        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating project {}: {}", projectId, e.getMessage());
            throw new BusinessRuleException(
                    "PROJECT-VALIDATION-ERROR",
                    "Could not validate project " + projectId + ". Please try again."
            );
        }
    }

    /**
     * Validate that budget amount doesn't exceed the project's total budget
     */
   private void validateBudgetAmountNotExceedingProjectBudget(String projectId, BigDecimal requestedAmount) {
       try {
           String authHeader = getAuthorizationHeader();
           if (authHeader == null) {
               return;  // Skip this validation if no auth header
           }

           ProjectDto project = projectServiceClient.getProject(projectId, authHeader);

           if (project == null) {
               throw new BusinessRuleException(
                       "PROJECT-NOT-FOUND",
                       "Project with ID " + projectId + " does not exist"
               );
           }

           Double projectBudget = project.budget() != null ? project.budget() : null;

           if (projectBudget == null) {
               log.warn("Project {} has no budget defined", projectId);
               return;
           }

           BigDecimal projectBudgetBD = BigDecimal.valueOf(projectBudget);

           // CONDITION 1: No single category budget should exceed the total project budget
           if (requestedAmount.compareTo(projectBudgetBD) > 0) {
               throw new BusinessRuleException(
                       "FIN-BUS-005",
                       "Single category budget amount " + requestedAmount + 
                       " exceeds total project budget limit of " + projectBudget +
                       ". Each category budget must not exceed the project's total allocated budget."
               );
           }

           // Get total allocated budget from existing budgets for this project
           BigDecimal totalAllocatedBudget = budgetRepository.findByProjectId(projectId, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                   .getContent()
                   .stream()
                   .map(Budget::getPlannedAmount)
                   .reduce(BigDecimal.ZERO, BigDecimal::add);

           // CONDITION 2: Sum of all category budgets CAN exceed by up to 50% more (allowed)
           // Only throw error if exceeds 50% MORE than project budget
           BigDecimal totalAfterCreation = totalAllocatedBudget.add(requestedAmount);
           BigDecimal fiftyPercentMoreThanBudget = projectBudgetBD.multiply(BigDecimal.valueOf(1.5));
           
           if (totalAfterCreation.compareTo(fiftyPercentMoreThanBudget) > 0) {
               throw new BusinessRuleException(
                       "FIN-BUS-006",
                       "Total budget allocation for all categories (" + totalAfterCreation + 
                       ") exceeds the allowed limit of 150% of project budget (" + fiftyPercentMoreThanBudget + 
                       "). Already allocated: " + totalAllocatedBudget + 
                       ", New request: " + requestedAmount +
                       ". Note: Sum can be up to 150% of project budget, but not more."
               );
           }

           // CONDITION 3: Sum of all category budgets should not exceed 50% of project budget (for safety check)
           BigDecimal fiftyPercentOfProjectBudget = projectBudgetBD.multiply(BigDecimal.valueOf(0.5));
           if (totalAfterCreation.compareTo(fiftyPercentOfProjectBudget) > 0) {
               log.warn("Budget allocation for project {} has exceeded 50% threshold. Total: {}, Threshold: {}", 
                       projectId, totalAfterCreation, fiftyPercentOfProjectBudget);
               // This is informational - not an error, just a warning
           }

           log.info("Budget amount validation passed for project: {}. Single budget: {}, Total allocated: {}, Max allowed (150%): {}", 
                   projectId, requestedAmount, totalAfterCreation, fiftyPercentMoreThanBudget);

       } catch (BusinessRuleException e) {
           throw e;
       } catch (Exception e) {
           log.error("Error validating budget amount for project {}: {}", projectId, e.getMessage());
           // Don't fail budget creation if we can't validate against project budget
           log.warn("Skipping budget amount validation due to error");
       }
   }

    /**
     * Extract authorization header from Security Context
     */
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

    /**
     * Validate that budget is in draft status before allowing edits
     */
    public void validateBudgetCanBeEdited(Budget budget) {
        if (!budget.getStatus().name().equals("DRAFT")) {
            throw new BusinessRuleException(
                    "FIN-BUS-003",
                    "Budget cannot be edited after approval"
            );
        }
    }

    /**
     * Validate that budget is not already approved
     */
    public void validateBudgetNotApproved(Budget budget) {
        if (budget.getStatus().name().equals("APPROVED")) {
            throw new BusinessRuleException(
                    "FIN-BUS-004",
                    "Budget is already approved and cannot be modified"
            );
        }
    }

    /**
     * Validate budget update request
     */
    public void validateBudgetUpdate(BudgetUpdateRequest request) {
        log.info("Validating budget update request");

        // Validate planned amount
        if (request.getPlannedAmount() == null || request.getPlannedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "FIN-VAL-011",
                    "plannedAmount",
                    "Planned amount must be greater than 0"
            );
        }

        // Validate category
        if (request.getBudgetCategory() == null) {
            throw new ValidationException(
                    "FIN-VAL-012",
                    "budgetCategory",
                    "Budget category is required"
            );
        }
    }

    /**
     * Validate that budget can be deleted (only DRAFT status allowed)
     */
    public void validateBudgetCanBeDeleted(Budget budget) {
        if (!budget.getStatus().name().equals("DRAFT")) {
            throw new BusinessRuleException(
                    "FIN-BUS-007",
                    "Budget cannot be deleted after approval. Current status: " + budget.getStatus()
            );
        }
    }
}
