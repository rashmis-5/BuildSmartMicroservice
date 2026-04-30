package com.buildsmart.finance.client;

import com.buildsmart.finance.client.dto.InvoiceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client for Vendor Microservice communication
 * Fetches invoice details for payment validation
 */
@FeignClient(name = "vendor-service")
public interface VendorClient {

    /**
     * Fetch invoice details by invoiceId
     *
     * @param invoiceId The invoice ID to fetch
     * @param authHeader Authorization header with Bearer token
     * @return Invoice details including amount, approvedBy, and status
     */
    @GetMapping("/api/invoices/{Id}")
    InvoiceDto getInvoice(
            @PathVariable String invoiceId,
            @RequestHeader("Authorization") String authHeader
    );
}

