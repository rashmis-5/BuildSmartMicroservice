package com.buildsmart.finance.service.impl;

import com.buildsmart.finance.client.VendorClient;
import com.buildsmart.finance.client.dto.InvoiceDto;
import com.buildsmart.finance.dto.request.PaymentApprovalRequest;
import com.buildsmart.finance.dto.request.PaymentCreateRequest;
import com.buildsmart.finance.dto.response.PagedResponse;
import com.buildsmart.finance.dto.response.PaymentResponse;
import com.buildsmart.finance.entity.Budget;
import com.buildsmart.finance.entity.Expense;
import com.buildsmart.finance.entity.Payment;
import com.buildsmart.finance.entity.enums.PaymentStatus;
import com.buildsmart.finance.exception.BusinessRuleException;
import com.buildsmart.finance.exception.ResourceNotFoundException;
import com.buildsmart.finance.repository.BudgetRepository;
import com.buildsmart.finance.repository.ExpenseRepository;
import com.buildsmart.finance.repository.PaymentRepository;
import com.buildsmart.finance.service.PaymentService;
import com.buildsmart.finance.util.IdGenerator;
import com.buildsmart.finance.util.JwtUtil;
import com.buildsmart.finance.validator.PaymentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final PaymentValidator paymentValidator;
    private final JwtUtil jwtUtil;
    private final VendorClient vendorClient;

    @Override
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        log.info("Creating payment for invoice: {}", request.getInvoiceId());

        // Fetch and validate invoice from vendor service
        String authHeader = getAuthorizationHeader();
        InvoiceDto invoice = vendorClient.getInvoice(request.getInvoiceId(), authHeader);
        
        if (invoice == null) {
            throw new ResourceNotFoundException(
                    "FIN-NOT-FOUND-007",
                    request.getInvoiceId(),
                    "Invoice not found with ID: " + request.getInvoiceId()
            );
        }

        // Validate invoice is approved and has correct approval
        if (invoice.approvedBy() == null || !invoice.approvedBy().trim().isEmpty()) {
            if (!"APPROVED".equalsIgnoreCase(invoice.status())) {
                throw new BusinessRuleException(
                        "FIN-BUS-010",
                        "Invoice must be APPROVED by vendor before payment can be processed"
                );
            }
        }

        // Validate expense exists and is approved
        Expense expense = expenseRepository.findById(request.getExpenseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-003",
                        request.getExpenseId(),
                        "Expense not found with ID: " + request.getExpenseId()
                ));

        paymentValidator.validatePaymentCreation(request, expense);
        paymentValidator.checkDuplicatePayment(request.getInvoiceId(), request.getAmount());

        // Validate payment amount matches invoice amount
        if (request.getAmount().compareTo(invoice.amount()) != 0) {
            throw new BusinessRuleException(
                    "FIN-BUS-011",
                    "Payment amount must match invoice amount. Invoice amount: " + invoice.amount() + 
                    ", Payment amount: " + request.getAmount()
            );
        }

        String paymentId = IdGenerator.generatePaymentId();
        
        // Auto-fill createdBy from JWT token
        String createdBy = extractUserIdFromToken();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .invoiceId(request.getInvoiceId())
                .expenseId(request.getExpenseId())
                .amount(invoice.amount())
                .paymentMethod(request.getPaymentMethod())
                .bankReferenceNumber(request.getBankReferenceNumber())
                .status(PaymentStatus.INITIATED)
                .createdBy(createdBy)
                .isDeleted(false)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created successfully with ID: {}", paymentId);

        publishPaymentInitiatedEvent(savedPayment);

        return mapToResponse(savedPayment);
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        log.info("Fetching payment with ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-004",
                        paymentId,
                        "Payment not found with ID: " + paymentId
                ));

        return mapToResponse(payment);
    }

    @Override
    public PagedResponse<PaymentResponse> getPaymentsByExpenseId(String expenseId, Pageable pageable) {
        log.info("Fetching payments for expense: {}", expenseId);

        Page<Payment> payments = paymentRepository.findByExpenseId(expenseId, pageable);
        return buildPagedResponse(payments);
    }

    @Override
    public PagedResponse<PaymentResponse> getPaymentsByStatus(String status, Pageable pageable) {
        log.info("Fetching payments with status: {}", status);

        PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
        Page<Payment> payments = paymentRepository.findByStatus(paymentStatus, pageable);
        return buildPagedResponse(payments);
    }

    @Override
    public PaymentResponse updatePaymentStatus(String paymentId, PaymentApprovalRequest request) {
        log.info("Updating payment status for ID: {} to: {}", paymentId, request.getStatus());

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FIN-NOT-FOUND-004",
                        paymentId,
                        "Payment not found with ID: " + paymentId
                ));

        if (request.getStatus() == PaymentStatus.REJECTED) {
            paymentValidator.validatePaymentCanBeRejected(payment.getStatus().name());
        }

        payment.setStatus(request.getStatus());
        payment.setApprovedBy(request.getApprovedBy());
        payment.setApprovedAt(LocalDateTime.now());

        if (request.getStatus() == PaymentStatus.REJECTED) {
            payment.setRejectionReason(request.getRejectionReason());
        }

        if (request.getStatus() == PaymentStatus.COMPLETED) {
            payment.setPaymentDate(LocalDateTime.now());
            updateBudgetActualAmount(payment);
        }

        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Payment {} status updated to: {}", paymentId, request.getStatus());

        publishPaymentStatusChangedEvent(updatedPayment);

        return mapToResponse(updatedPayment);
    }

    @Override
    public PagedResponse<PaymentResponse> getPaymentsByCreatedBy(String createdBy, Pageable pageable) {
        log.info("Fetching payments created by: {}", createdBy);

        Page<Payment> payments = paymentRepository.findByCreatedBy(createdBy, pageable);
        return buildPagedResponse(payments);
    }

    @Override
    public PagedResponse<PaymentResponse> getPendingPayments(Pageable pageable) {
        log.info("Fetching pending payments");

        List<Payment> pendingPayments = paymentRepository.findPendingPayments();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), pendingPayments.size());

        List<Payment> pagedPayments = new ArrayList<>();
        if (start < pendingPayments.size()) {
            pagedPayments = pendingPayments.subList(start, end);
        }

        Page<Payment> page = new PageImpl<>(pagedPayments, pageable, pendingPayments.size());
        return buildPagedResponse(page);
    }

    private void updateBudgetActualAmount(Payment payment) {
        log.info("Updating budget actual amount for payment: {}", payment.getPaymentId());

        var expenseOpt = expenseRepository.findById(payment.getExpenseId());
        if (expenseOpt.isEmpty()) {
            log.warn("Expense not found for payment: {}", payment.getPaymentId());
            return;
        }

        Expense expense = expenseOpt.get();
        var budgetOpt = budgetRepository.findById(expense.getBudgetId());

        if (budgetOpt.isEmpty()) {
            log.warn("Budget not found for expense: {}", expense.getExpenseId());
            return;
        }

        Budget budget = budgetOpt.get();

        // Add payment amount to actual amount
        budget.setActualAmount(budget.getActualAmount().add(payment.getAmount()));
        budget.calculateVariance();

        budgetRepository.save(budget);
        log.info("Budget actual amount updated for budget: {}", budget.getBudgetId());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .invoiceId(payment.getInvoiceId())
                .expenseId(payment.getExpenseId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .bankReferenceNumber(payment.getBankReferenceNumber())
                .createdBy(payment.getCreatedBy())
                .approvedBy(payment.getApprovedBy())
                .approvedAt(payment.getApprovedAt())
                .rejectionReason(payment.getRejectionReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private PagedResponse<PaymentResponse> buildPagedResponse(Page<Payment> page) {
        return PagedResponse.<PaymentResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void publishPaymentInitiatedEvent(Payment payment) {
        log.info("Publishing PaymentInitiatedEvent for payment: {}", payment.getPaymentId());
    }

    private void publishPaymentStatusChangedEvent(Payment payment) {
        log.info("Publishing PaymentStatusChangedEvent for payment: {}", payment.getPaymentId());
    }

    /**
     * Get Authorization header from current security context
     *
     * @return Authorization header with Bearer token
     */
    private String getAuthorizationHeader() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object credentials = authentication.getCredentials();
                if (credentials != null) {
                    String token = credentials.toString();
                    String bearerToken = token.startsWith("Bearer ") ? token : "Bearer " + token;
                    log.debug("Extracted authorization header from security context");
                    return bearerToken;
                }
            }
            log.warn("Could not extract authorization header from security context");
            return null;
        } catch (Exception e) {
            log.error("Error extracting authorization header: {}", e.getMessage());
            return null;
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
}
