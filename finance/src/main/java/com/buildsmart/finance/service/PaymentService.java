package com.buildsmart.finance.service;

import com.buildsmart.finance.dto.request.PaymentApprovalRequest;
import com.buildsmart.finance.dto.request.PaymentCreateRequest;
import com.buildsmart.finance.dto.response.PagedResponse;
import com.buildsmart.finance.dto.response.PaymentResponse;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    /**
     * Create a new payment
     */
    PaymentResponse createPayment(PaymentCreateRequest request);

    /**
     * Get payment by ID
     */
    PaymentResponse getPaymentById(String paymentId);

    /**
     * Get all payments for an expense with pagination
     */
    PagedResponse<PaymentResponse> getPaymentsByExpenseId(String expenseId, Pageable pageable);

    /**
     * Get all payments by status with pagination
     */
    PagedResponse<PaymentResponse> getPaymentsByStatus(String status, Pageable pageable);

    /**
     * Approve/Reject/Complete payment (Payment status update)
     */
    PaymentResponse updatePaymentStatus(String paymentId, PaymentApprovalRequest request);

    /**
     * Get payments created by user
     */
    PagedResponse<PaymentResponse> getPaymentsByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Get pending payments
     */
    PagedResponse<PaymentResponse> getPendingPayments(Pageable pageable);
}
