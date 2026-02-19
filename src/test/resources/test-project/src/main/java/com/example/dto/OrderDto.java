package com.example.dto;

/**
 * 주문 정보 DTO (inner class enum 테스트용)
 */
public class OrderDto {

    /**
     * 주문 ID
     */
    private Long orderId;

    /**
     * 주문 상태
     */
    private OrderStatus status;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * 주문 상태 enum
     */
    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }
}
