package com.ecom.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long LOCK_TIMEOUT_SECONDS = 10;

    public boolean acquireLock(String lockKey) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

        if (Boolean.TRUE.equals(acquired)) {
            log.info("Lock acquired: {}", lockKey);
            return true;
        }

        log.warn("Failed to acquire lock: {}", lockKey);
        return false;
    }

    public void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
        log.info("Lock released: {}", lockKey);
    }
}