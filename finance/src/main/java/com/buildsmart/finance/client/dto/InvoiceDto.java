package com.buildsmart.finance.client.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Invoice from Vendor Microservice
 */
public record InvoiceDto(
        String invoiceId,
        String vendorId,
        BigDecimal amount,
        String approvedBy,
        String status
) {}
