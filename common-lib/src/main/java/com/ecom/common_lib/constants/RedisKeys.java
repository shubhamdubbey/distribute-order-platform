package com.ecom.common_lib.constants;

public final class RedisKeys {

    private RedisKeys() {}

    public static String inventoryKey(String productId) {
        return "inventory:product:" + productId;
    }

    public static String orderLockKey(String userId) {
        return "lock:order:user:" + userId;
    }

    public static String idempotencyKey(String requestId) {
        return "idempotency:order:" + requestId;
    }
}