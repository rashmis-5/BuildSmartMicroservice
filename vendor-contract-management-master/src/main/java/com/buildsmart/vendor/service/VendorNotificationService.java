package com.buildsmart.vendor.service;

import com.buildsmart.vendor.entity.VendorNotification;
import com.buildsmart.vendor.enums.VendorNotificationType;

public interface VendorNotificationService {
    void createNotification(String vendorId, String message, VendorNotificationType type);

    void createNotification(String vendorId, String projectId, String taskId, String title, String message, VendorNotificationType type);
    java.util.List<VendorNotification> getNotificationsByVendorId(String vendorId);
    void markAsRead(Long notificationId);
    long getUnreadCount(String vendorId);
}
