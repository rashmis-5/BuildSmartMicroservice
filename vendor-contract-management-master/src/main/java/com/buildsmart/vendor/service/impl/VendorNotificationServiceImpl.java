package com.buildsmart.vendor.service.impl;

import com.buildsmart.vendor.entity.VendorNotification;
import com.buildsmart.vendor.enums.VendorNotificationType;
import com.buildsmart.vendor.repository.VendorNotificationRepository;
import com.buildsmart.vendor.service.VendorNotificationService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class VendorNotificationServiceImpl implements VendorNotificationService {

    private static final Logger log = LoggerFactory.getLogger(VendorNotificationServiceImpl.class);
    private final VendorNotificationRepository notificationRepository;

    public VendorNotificationServiceImpl(VendorNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }


    @Override
    public void createNotification(String vendorId, String message, VendorNotificationType type) {
        createNotification(vendorId, null, null, null, message, type);
    }

    @Override
    public void createNotification(String vendorId, String projectId, String taskId, String title, String message, VendorNotificationType type) {
        try {
            VendorNotification notification = new VendorNotification();
            notification.setVendorId(vendorId);
            notification.setProjectId(projectId);
            notification.setRelatedTaskId(taskId);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type);
            notificationRepository.save(notification);
            log.info("Notification created for vendorId={}, type={}, title={}", vendorId, type, title);
        } catch (Exception e) {
            log.error("Failed to create notification for vendorId={}, type={}, title={}, error={}", vendorId, type, title, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<VendorNotification> getNotificationsByVendorId(String vendorId) {
        return notificationRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    @Override
    public long getUnreadCount(String vendorId) {
        return notificationRepository.countByVendorIdAndIsReadFalse(vendorId);
    }
}
