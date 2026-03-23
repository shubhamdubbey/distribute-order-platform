package com.ecom.order_service.service;

import com.ecom.common_lib.constants.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Why Lua script instead of just checking then decrementing?
 *
 * This is a critical interview point. Even though Redis is single-threaded, two separate commands — GET then DECRBY — are not atomic together. Between your GET and DECRBY, another client could decrement the stock. The window is tiny but real at high concurrency.
 *
 * A Lua script executes as a single atomic unit in Redis. Nobody can interleave between the check and the decrement. This is the correct production-grade approach.
 */
@Service
@Slf4j
public class InventoryRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    public InventoryRedisService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void seedInventory(String productId, int quantity) {
        String key = RedisKeys.inventoryKey(productId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(quantity));
        log.info("Inventory seeded for productId: {} with quantity: {}", productId, quantity);
    }

    public boolean decrementStock(String productId, int quantity) {
        String key = RedisKeys.inventoryKey(productId);

        String luaScript = """
                local current = tonumber(redis.call('GET', KEYS[1]))
                if current == nil then
                    return -1
                end
                if current < tonumber(ARGV[1]) then
                    return -2
                end
                return redis.call('DECRBY', KEYS[1], ARGV[1])
                """;

        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                List.of(key),
                String.valueOf(quantity)
        );

        if (result == null || result == -1L) {
            log.warn("Product not found in inventory cache: {}", productId);
            return false;
        }

        if (result == -2L) {
            log.warn("Insufficient stock for productId: {}. Requested: {}", productId, quantity);
            return false;
        }

        log.info("Stock decremented for productId: {}. Remaining: {}", productId, result);
        return true;
    }

    public Long getStock(String productId) {
        String key = RedisKeys.inventoryKey(productId);
        log.info("Fetching stock for key: {}", key);
        String value = stringRedisTemplate.opsForValue().get(key);
        log.info("Raw value from Redis for key {}: {}", key, value);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public void restoreStock(String productId, int quantity) {
        String key = RedisKeys.inventoryKey(productId);
        stringRedisTemplate.opsForValue().increment(key, quantity);
        log.info("Stock restored for productId: {}. Restored quantity: {}", productId, quantity);
    }
}