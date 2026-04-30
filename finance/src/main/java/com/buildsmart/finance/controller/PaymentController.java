package com.buildsmart.finance.controller;

import com.buildsmart.finance.dto.request.PaymentApprovalRequest;
import com.buildsmart.finance.dto.request.PaymentCreateRequest;
import com.buildsmart.finance.dto.response.PagedResponse;
import com.buildsmart.finance.dto.response.PaymentResponse;
import com.buildsmart.finance.service.PaymentService;
import com.buildsmart.finance.util.PaginationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment
     * POST /api/payments
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request) {
        log.info("POST /api/payments - Creating payment");
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get payment by ID
     * GET /api/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        log.info("GET /api/payments/{} - Fetching payment", paymentId);
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payments for an expense with pagination
     * GET /api/payments/expenses/{expenseId}?page=0&size=10
     */
    @GetMapping("/expenses/{expenseId}")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPaymentsByExpense(
            @PathVariable String expenseId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/payments/expenses/{} - Fetching payments", expenseId);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<PaymentResponse> response = paymentService.getPaymentsByExpenseId(expenseId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payments by status with pagination
     * GET /api/payments/status/{status}?page=0&size=10
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPaymentsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/payments/status/{} - Fetching payments", status);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<PaymentResponse> response = paymentService.getPaymentsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Update payment status
     * POST /api/payments/{paymentId}/status
     */
    @PostMapping("/{paymentId}/status")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentApprovalRequest request) {
        log.info("POST /api/payments/{}/status - Updating payment status", paymentId);
        PaymentResponse response = paymentService.updatePaymentStatus(paymentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payments by creator
     * GET /api/payments/users/{createdBy}?page=0&size=10
     */
    @GetMapping("/users/{createdBy}")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPaymentsByCreatedBy(
            @PathVariable String createdBy,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/payments/users/{} - Fetching payments", createdBy);

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<PaymentResponse> response = paymentService.getPaymentsByCreatedBy(createdBy, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get pending payments
     * GET /api/payments/pending?page=0&size=10
     */
    @GetMapping("/pending")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPendingPayments(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        log.info("GET /api/payments/pending - Fetching pending payments");

        Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, sortOrder);
        PagedResponse<PaymentResponse> response = paymentService.getPendingPayments(pageable);
        return ResponseEntity.ok(response);
    }
}
