package com.ecom.order_service.exception;

public class OrderLockException extends RuntimeException {
    public OrderLockException(String message) {
        super(message);
    }
}