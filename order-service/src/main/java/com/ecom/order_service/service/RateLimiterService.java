package com.ecom.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int MAX_TOKENS = 5;
    private static final int REFILL_INTERVAL_SECONDS = 60;

    // Lua script — atomic check and decrement
    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local max_tokens = tonumber(ARGV[1])
            local refill_interval = tonumber(ARGV[2])
                        
            local current = redis.call('GET', key)
                        
            if current == false then
                -- Key doesn't exist — first request from this user
                -- Set max tokens and decrement by 1
                redis.call('SET', key, max_tokens - 1)
                redis.call('EXPIRE', key, refill_interval)
                return max_tokens - 1
            end
                        
            local tokens = tonumber(current)
                        
            if tokens <= 0 then
                -- No tokens left — reject
                return -1
            end
                        
            -- Has tokens — decrement and allow
            redis.call('DECR', key)
            return tokens - 1
            """;

    public RateLimitResult tryConsume(String userId) {
        String key = "ratelimit:user:" + userId;

        Long remainingTokens = stringRedisTemplate.execute(
                new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class),
                List.of(key),
                String.valueOf(MAX_TOKENS),
                String.valueOf(REFILL_INTERVAL_SECONDS)
        );

        if (remainingTokens == null || remainingTokens == -1) {
            log.warn("Rate limit exceeded for userId: {}", userId);
            return RateLimitResult.rejected(userId);
        }

        log.debug("Rate limit check passed for userId: {}. Tokens remaining: {}",
                userId, remainingTokens);
        return RateLimitResult.allowed(remainingTokens);
    }

    // Result object — clean way to return multiple values
    public record RateLimitResult(
            boolean allowed,
            long remainingTokens,
            String userId
    ) {
        static RateLimitResult allowed(long remaining) {
            return new RateLimitResult(true, remaining, null);
        }

        static RateLimitResult rejected(String userId) {
            return new RateLimitResult(false, 0, userId);
        }
    }
}