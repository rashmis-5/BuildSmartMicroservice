package com.buildsmart.finance.validator;

import com.buildsmart.finance.dto.request.PaymentCreateRequest;
import com.buildsmart.finance.entity.Expense;
import com.buildsmart.finance.entity.Payment;
import com.buildsmart.finance.exception.BusinessRuleException;
import com.buildsmart.finance.exception.ValidationException;
import com.buildsmart.finance.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentValidator {

    private final PaymentRepository paymentRepository;

    /**
     * Validate payment creation request
     */
    public void validatePaymentCreation(PaymentCreateRequest request, Expense expense) {
        log.info("Validating payment creation request for invoice: {}", request.getInvoiceId());

        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(
                    "FIN-VAL-005",
                    "amount",
                    "Payment amount must be greater than 0"
            );
        }

        // Check if expense is approved
        if (!expense.getStatus().name().equals("APPROVED")) {
            throw new BusinessRuleException(
                    "FIN-BUS-008",
                    "Expense must be APPROVED before payment can be processed"
            );
        }

        // Check if amount doesn't exceed expense amount
        if (request.getAmount().compareTo(expense.getAmount()) > 0) {
            throw new BusinessRuleException(
                    "FIN-BUS-009",
                    "Payment amount cannot exceed expense amount"
            );
        }
    }

    /**
     * Check for duplicate payment (idempotency)
     */
    public void checkDuplicatePayment(String invoiceId, BigDecimal amount) {
        var existingPayment = paymentRepository.findByInvoiceIdAndAmount(invoiceId, amount);
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            if (!payment.getStatus().name().equals("REJECTED")) {
                throw new BusinessRuleException(
                        "FIN-BUS-010",
                        "A payment already exists for this invoice with this amount. ID: " + payment.getPaymentId()
                );
            }
        }
    }

    /**
     * Validate payment can be rejected
     */
    public void validatePaymentCanBeRejected(String status) {
        if (status.equals("COMPLETED") || status.equals("REJECTED")) {
            throw new BusinessRuleException(
                    "FIN-BUS-011",
                    "Payment in " + status + " status cannot be rejected"
            );
        }
    }
}
