package com.buildsmart.vendor.repository;

import com.buildsmart.vendor.enums.ContractStatus;
import com.buildsmart.vendor.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, String> {
    List<Contract> findByVendorId(String vendorId);
    List<Contract> findByStatus(ContractStatus status);
    Optional<Contract> findTopByOrderByContractIdDesc();

}
