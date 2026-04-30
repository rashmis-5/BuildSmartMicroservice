package com.buildsmart.vendor.service;

import com.buildsmart.vendor.dto.response.DeliveryResponse;
import com.buildsmart.vendor.dto.request.DeliveryRequest;
import com.buildsmart.vendor.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface DeliveryService {
    Page<DeliveryResponse> getAllDeliveries(Pageable pageable);
    DeliveryResponse getDeliveryById(String id);
    List<DeliveryResponse> getDeliveriesByContractId(String contractId);
    List<DeliveryResponse> getDeliveriesByStatus(DeliveryStatus status);
    DeliveryResponse createDelivery(DeliveryRequest request);
    DeliveryResponse updateDelivery(String id, DeliveryRequest request);
    void deleteDelivery(String id);
}
