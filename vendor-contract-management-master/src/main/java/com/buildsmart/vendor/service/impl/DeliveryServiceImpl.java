package com.buildsmart.vendor.service.impl;

import com.buildsmart.vendor.dto.response.DeliveryResponse;
import com.buildsmart.vendor.dto.request.DeliveryRequest;
import com.buildsmart.vendor.enums.DeliveryStatus;
import com.buildsmart.vendor.exception.CustomExceptions.DeliveryNotFoundException;
import com.buildsmart.vendor.entity.Delivery;
import com.buildsmart.vendor.repository.DeliveryRepository;
import com.buildsmart.vendor.service.DeliveryService;
import com.buildsmart.vendor.util.IdGeneratorUtil;
import com.buildsmart.vendor.validator.DeliveryValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private DeliveryValidator deliveryValidator;

    @Autowired
    private com.buildsmart.vendor.repository.ContractRepository contractRepository;

    @Autowired
    private com.buildsmart.vendor.validator.ProjectDateValidator projectDateValidator;

    @Override
    public Page<DeliveryResponse> getAllDeliveries(Pageable pageable) {
        return deliveryRepository.findAll(pageable).map(this::toDTO);
    }

    @Override
    public DeliveryResponse getDeliveryById(String id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new DeliveryNotFoundException(id));
        return toDTO(delivery);
    }

    @Override
    public List<DeliveryResponse> getDeliveriesByContractId(String contractId) {
        List<Delivery> deliveries = deliveryRepository.findByContractId(contractId);
        List<DeliveryResponse> dtoList = new ArrayList<>();
        for (Delivery delivery : deliveries) {
            dtoList.add(toDTO(delivery));
        }
        return dtoList;
    }

    @Override
    public List<DeliveryResponse> getDeliveriesByStatus(DeliveryStatus status) {
        List<Delivery> deliveries = deliveryRepository.findByStatus(status);
        List<DeliveryResponse> dtoList = new ArrayList<>();
        for (Delivery delivery : deliveries) {
            dtoList.add(toDTO(delivery));
        }
        return dtoList;
    }

    @Override
    public DeliveryResponse createDelivery(DeliveryRequest request) {
        deliveryValidator.validate(request);

        // Project-window enforcement (Item #3): delivery date must fall inside
        // the parent project's window (resolved via the contract).
        com.buildsmart.vendor.entity.Contract parentContract =
                contractRepository.findById(request.getContractId()).orElse(null);
        if (parentContract != null && parentContract.getProjectId() != null) {
            projectDateValidator.validateDateWithinProject(
                    parentContract.getProjectId(), request.getDate(), "Delivery date");
        }

        String lastId = deliveryRepository.findTopByOrderByDeliveryIdDesc()
                .map(Delivery::getDeliveryId)
                .orElse(null);
        Delivery delivery = new Delivery();
        delivery.setDeliveryId(IdGeneratorUtil.nextDeliveryId(lastId));
        delivery.setContractId(request.getContractId());
        delivery.setDate(request.getDate());
        delivery.setItem(request.getItem());
        delivery.setQuantity(request.getQuantity());
        delivery.setStatus(request.getStatus());
        Delivery saved = deliveryRepository.save(delivery);
        return toDTO(saved);
    }

    @Override
    public DeliveryResponse updateDelivery(String id, DeliveryRequest request) {
        deliveryValidator.validate(request);

        // Project-window enforcement (Item #3): updated delivery date must
        // remain inside the project window.
        com.buildsmart.vendor.entity.Contract parentContract =
                contractRepository.findById(request.getContractId()).orElse(null);
        if (parentContract != null && parentContract.getProjectId() != null) {
            projectDateValidator.validateDateWithinProject(
                    parentContract.getProjectId(), request.getDate(), "Delivery date");
        }

        Delivery existing = deliveryRepository.findById(id)
                .orElseThrow(() -> new DeliveryNotFoundException(id));
        existing.setContractId(request.getContractId());
        existing.setDate(request.getDate());
        existing.setItem(request.getItem());
        existing.setQuantity(request.getQuantity());
        existing.setStatus(request.getStatus());
        Delivery updated = deliveryRepository.save(existing);
        return toDTO(updated);
    }

    @Override
    public void deleteDelivery(String id) {
        deliveryRepository.deleteById(id);
    }

    private DeliveryResponse toDTO(Delivery delivery) {
        DeliveryResponse dto = new DeliveryResponse();
        dto.setDeliveryId(delivery.getDeliveryId());
        dto.setContractId(delivery.getContractId());
        dto.setDate(delivery.getDate());
        dto.setItem(delivery.getItem());
        dto.setQuantity(delivery.getQuantity());
        dto.setStatus(delivery.getStatus());
        return dto;
    }
}
