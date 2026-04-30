package com.buildsmart.finance.validator;

import com.buildsmart.finance.dto.request.ExpenseCreateRequest;
import com.buildsmart.finance.dto.request.ExpenseUpdateRequest;
import com.buildsmart.finance.entity.Budget;
import com.buildsmart.finance.entity.Expense;
import com.buildsmart.finance.exception.BusinessRuleException;
import com.buildsmart.finance.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseValidator {

    /**
     * Validate expense creation request
     */
    public void validateExpenseCreation(ExpenseCreateRequest request, Budget budget) {
        log.info("Validating expense creation request for budget: {}", request.getBudgetId());

        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "FIN-VAL-004",
                    "amount",
                    "Amount must be greater than 0"
            );
        }

        // Check if budget is approved
        if (!budget.getStatus().name().equals("APPROVED")) {
            throw new BusinessRuleException(
                    "FIN-BUS-005",
                    "Budget must be APPROVED before adding expenses"
            );
        }
    }

    /**
     * Validate that expense can be submitted
     */
    public void validateExpenseCanBeSubmitted(String expenseStatus) {
        if (!expenseStatus.equals("DRAFT")) {
            throw new BusinessRuleException(
                    "FIN-BUS-006",
                    "Only DRAFT expenses can be submitted"
            );
        }
    }

    /**
     * Validate that expense is approved
     */
    public void validateExpenseIsApproved(String expenseStatus) {
        if (!expenseStatus.equals("APPROVED")) {
            throw new BusinessRuleException(
                    "FIN-BUS-007",
                    "Expense must be APPROVED for payment processing"
            );
        }
    }

    /**
     * Validate expense update request
     */
    public void validateExpenseUpdate(ExpenseUpdateRequest request) {
        log.info("Validating expense update request");

        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "FIN-VAL-013",
                    "amount",
                    "Amount must be greater than 0"
            );
        }

        // Validate description
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new ValidationException(
                    "FIN-VAL-014",
                    "description",
                    "Description is required"
            );
        }

        // Validate expense date
        if (request.getExpenseDate() == null) {
            throw new ValidationException(
                    "FIN-VAL-015",
                    "expenseDate",
                    "Expense date is required"
            );
        }
    }

    /**
     * Validate that expense can be edited (only DRAFT status allowed)
     */
    public void validateExpenseCanBeEdited(Expense expense) {
        if (!expense.getStatus().name().equals("DRAFT")) {
            throw new BusinessRuleException(
                    "FIN-BUS-008",
                    "Expense cannot be edited after submission. Current status: " + expense.getStatus()
            );
        }
    }

    /**
     * Validate that expense can be deleted (only DRAFT status allowed)
     */
    public void validateExpenseCanBeDeleted(Expense expense) {
        if (!expense.getStatus().name().equals("DRAFT")) {
            throw new BusinessRuleException(
                    "FIN-BUS-009",
                    "Expense cannot be deleted after submission. Current status: " + expense.getStatus()
            );
        }
    }
}
