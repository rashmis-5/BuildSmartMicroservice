package com.buildsmart.vendor.repository;

import com.buildsmart.vendor.entity.VendorNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorNotificationRepository extends JpaRepository<VendorNotification, Long> {
    List<VendorNotification> findByVendorIdOrderByCreatedAtDesc(String vendorId);
    long countByVendorIdAndIsReadFalse(String vendorId);
}
