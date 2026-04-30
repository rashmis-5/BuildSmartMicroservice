package com.company.notification.enums;

/**
 * Generic event types. The notification service does NOT know the business
 * meaning — it only stores and routes. Add new types here without changing
 * any business logic in this service.
 *
 * Stored as a String in the DB (length 64) for forward compatibility:
 * a producer can send a new event type and old consumers won't break.
 */
public enum EventType {
    TASK_CREATED,
    TASK_ASSIGNED,
    TASK_COMPLETED,
    WORK_SUBMITTED,
    INVOICE_CREATED,
    INVOICE_SUBMITTED,
    CONTRACT_UPLOADED,
    APPROVAL_REQUIRED,
    APPROVAL_GRANTED,
    APPROVAL_REJECTED,
    PAYMENT_RELEASED,
    GENERIC
}
