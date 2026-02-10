package com.project.usermanagement.enums;

/**
 * Enum representing different states of an order.
 */
public enum OrderStatus {
    PENDING, // Order created, awaiting confirmation
    CONFIRMED, // Order confirmed by admin/system
    PROCESSING, // Order is being prepared
    SHIPPED, // Order has been shipped
    DELIVERED, // Order successfully delivered
    CANCELLED // Order cancelled
}