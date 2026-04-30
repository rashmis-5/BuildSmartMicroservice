package com.buildsmart.vendor.service.impl;

import com.buildsmart.vendor.client.ProjectManagerClient;
import com.buildsmart.vendor.dto.response.ContractResponse;
import com.buildsmart.vendor.dto.request.ContractRequest;
import com.buildsmart.vendor.enums.ContractStatus;
import com.buildsmart.vendor.enums.VendorNotificationType;
import com.buildsmart.vendor.exception.CustomExceptions.ContractNotFoundException;
import com.buildsmart.vendor.exception.CustomExceptions.ProjectNotFoundException;
import com.buildsmart.vendor.entity.Contract;
import com.buildsmart.vendor.repository.ContractRepository;
import com.buildsmart.vendor.service.ContractService;
import com.buildsmart.vendor.service.VendorNotificationService;
import com.buildsmart.vendor.util.IdGeneratorUtil;
import com.buildsmart.vendor.validator.ContractValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContractServiceImpl implements ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractServiceImpl.class);

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractValidator contractValidator;

    @Autowired
    private VendorNotificationService vendorNotificationService;

    @Autowired
    private ProjectManagerClient projectManagerClient;

    @Autowired
    private com.buildsmart.vendor.service.VendorOwnershipService vendorOwnershipService;

    @Override
    public Page<ContractResponse> getAllContracts(Pageable pageable) {
        return contractRepository.findAll(pageable).map(this::toDTO);
    }

    @Override
    public ContractResponse getContractById(String id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));
        return toDTO(contract);
    }

    @Override
    public List<ContractResponse> getContractsByVendorId(String vendorId) {
        List<Contract> contracts = contractRepository.findByVendorId(vendorId);
        List<ContractResponse> dtoList = new ArrayList<>();
        for (Contract contract : contracts) {
            dtoList.add(toDTO(contract));
        }
        return dtoList;
    }

    @Override
    public List<ContractResponse> getContractsByStatus(ContractStatus status) {
        List<Contract> contracts = contractRepository.findByStatus(status);
        List<ContractResponse> dtoList = new ArrayList<>();
        for (Contract contract : contracts) {
            dtoList.add(toDTO(contract));;
        }
        return dtoList;
    }

    @Override
    public ContractResponse createContract(ContractRequest request, String vendorId) {
        // Hard precondition (Item: contract-create must be linked to a real PM project):
        // the project referenced by this contract MUST exist in the Project Manager
        // module. We fetch it eagerly so the vendor sees a clear "Project not found"
        // error instead of a silently-skipped PM notification.
        //
        // - PM 404           → ProjectNotFoundException → 404 to vendor.
        // - PM unreachable   → fallback returns null → log + degrade (don't punish
        //                       the vendor for PM being down; mirrors date-validator policy).
        // - Project found    → proceed with normal validation + save.
        if (request.getProjectId() != null && !request.getProjectId().isBlank()) {
            try {
                com.buildsmart.vendor.client.dto.ProjectDTO project =
                        projectManagerClient.getProjectById(request.getProjectId());
                if (project == null) {
                    log.warn("PM unreachable while verifying projectId={} for contract creation; proceeding without strict check",
                            request.getProjectId());
                }
            } catch (feign.FeignException.NotFound nf) {
                throw new ProjectNotFoundException(request.getProjectId());
            } catch (feign.FeignException fe) {
                // Non-404 transport errors: degrade rather than block.
                log.warn("PM transport error while verifying projectId={}: {}", request.getProjectId(), fe.getMessage());
            }
        }

        // Item 5: vendor ownership check. vendorId is the authenticated vendor
        // resolved from the JWT in ContractController. If a taskId is supplied,
        // verify both project and task ownership in a single PM call; otherwise
        // just project.
        if (request.getTaskId() != null && !request.getTaskId().isBlank()) {
            vendorOwnershipService.requireTaskOwnedByVendor(
                    request.getProjectId(), request.getTaskId(), vendorId);
        } else {
            vendorOwnershipService.requireProjectOwnedByVendor(
                    request.getProjectId(), vendorId);
        }

        contractValidator.validate(request, vendorId);

        String lastId = contractRepository.findTopByOrderByContractIdDesc()
                .map(Contract::getContractId)
                .orElse(null);
        Contract contract = new Contract();
        contract.setContractId(IdGeneratorUtil.nextContractId(lastId));
        contract.setVendorId(vendorId);
        contract.setProjectId(request.getProjectId());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setValue(request.getValue());
        contract.setStatus(request.getStatus());
        contract.setTaskId(request.getTaskId());
        Contract saved = contractRepository.save(contract);

        // --- Notify PM: contract created with taskId ---
        try {
            projectManagerClient.notifyContractCreated(
                    saved.getContractId(),
                    saved.getVendorId(),
                    saved.getProjectId(),
                    saved.getTaskId()
            );
            log.info("PM notified of new contract: contractId={}, taskId={}", saved.getContractId(), saved.getTaskId());
        } catch (Exception e) {
            log.warn("Failed to notify PM of contract creation (non-critical): contractId={}, reason={}",
                    saved.getContractId(), e.getMessage());
        }
        // --- Notify Vendor: contract created with taskId ---
        String vendorMsg = String.format(
                "A new contract (%s) has been created for Task ID: %s under Project: %s. " +
                "Contract period: %s to %s. Please review your contract details.",
                saved.getContractId(),
                saved.getTaskId() != null ? saved.getTaskId() : "N/A",
                saved.getProjectId(),
                saved.getStartDate(),
                saved.getEndDate()
        );
        try {
            vendorNotificationService.createNotification(
                    saved.getVendorId(),
                    vendorMsg,
                    VendorNotificationType.CONTRACT_CREATED
            );
            log.info("Vendor notified of new contract: contractId={}, vendorId={}, taskId={}",
                    saved.getContractId(), saved.getVendorId(), saved.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send vendor notification for contract creation: contractId={}", saved.getContractId(), e);
        }
        return toDTO(saved);
    }

    @Override
    public ContractResponse updateContract(String id, ContractRequest request, String vendorId) {
        // Item 5: ownership check on the post-update target. Same logic as
        // createContract — the vendor must own whatever project/task they
        // are pointing the contract at after the update.
        if (request.getTaskId() != null && !request.getTaskId().isBlank()) {
            vendorOwnershipService.requireTaskOwnedByVendor(
                    request.getProjectId(), request.getTaskId(), vendorId);
        } else {
            vendorOwnershipService.requireProjectOwnedByVendor(
                    request.getProjectId(), vendorId);
        }

        contractValidator.validate(request, vendorId);
        Contract existing = contractRepository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));

        existing.setVendorId(vendorId);
        existing.setProjectId(request.getProjectId());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setValue(request.getValue());
        existing.setStatus(request.getStatus());
        existing.setTaskId(request.getTaskId());
        Contract updated = contractRepository.save(existing);
        return toDTO(updated);
    }

    @Override
    public void deleteContract(String id) {
        contractRepository.deleteById(id);
    }

    private ContractResponse toDTO(Contract contract) {
        ContractResponse dto = new ContractResponse();
        dto.setContractId(contract.getContractId());
        dto.setVendorId(contract.getVendorId());
        dto.setProjectId(contract.getProjectId());
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setValue(contract.getValue());
        dto.setStatus(contract.getStatus());
        dto.setTaskId(contract.getTaskId());
        return dto;
    }
}
