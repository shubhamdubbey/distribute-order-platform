package com.ecom.order_service.service;

import com.ecom.common_lib.constants.RedisKeys;
import com.ecom.order_service.dto.CreateOrderRequest;
import com.ecom.order_service.dto.CreateOrderResponse;
import com.ecom.order_service.entity.Order;
import com.ecom.order_service.entity.OrderStatus;
import com.ecom.order_service.exception.DuplicateOrderException;
import com.ecom.order_service.exception.InsufficientStockException;
import com.ecom.order_service.exception.OrderLockException;
import com.ecom.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisLockService redisLockService;
    private final InventoryRedisService inventoryRedisService;

    public CreateOrderResponse createOrder(CreateOrderRequest request) {

        // Step 1: Idempotency check
        String idempotencyKey = RedisKeys.idempotencyKey(request.getRequestId());

        Boolean isNewRequest = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "PROCESSING", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(isNewRequest)) {
            log.warn("Duplicate request detected for requestId: {}", request.getRequestId());
            throw new DuplicateOrderException("Order already exists for requestId: "
                    + request.getRequestId());
        }

        // Step 2: Acquire distributed lock per user
        String lockKey = RedisKeys.orderLockKey(request.getUserId().toString());
        boolean lockAcquired = redisLockService.acquireLock(lockKey);

        if (!lockAcquired) {
            // Lock failed — delete idempotency key so client can retry
            redisTemplate.delete(idempotencyKey);
            throw new OrderLockException("Another order is being processed for this user. "
                    + "Please try again.");
        }

        try {
            // Step 3: Check and decrement inventory atomically
            boolean stockReserved = inventoryRedisService.decrementStock(
                    request.getProductId().toString(),
                    request.getQuantity()
            );

            if (!stockReserved) {
                // Stock failed — delete idempotency key so client can retry
                // Do NOT keep the key — this wasn't a successful processing
                redisTemplate.delete(idempotencyKey);
                throw new InsufficientStockException("Insufficient stock for product: "
                        + request.getProductId());
            }

            // Step 4: Save order
            Order order = Order.builder()
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .status(OrderStatus.PENDING)
                    .build();

            Order savedOrder = orderRepository.save(order);

            // Step 5: Update idempotency key with final orderId
            // This marks the request as fully processed
            redisTemplate.opsForValue().set(
                    idempotencyKey,
                    savedOrder.getId().toString(),
                    Duration.ofMinutes(10)
            );

            log.info("Order created successfully. orderId: {}", savedOrder.getId());

            return CreateOrderResponse.builder()
                    .orderId(savedOrder.getId())
                    .status(savedOrder.getStatus().name())
                    .message("Order placed successfully")
                    .build();

        } catch (InsufficientStockException | OrderLockException e) {
            // These are business exceptions — already handled above
            throw e;

        } catch (Exception e) {
            // Unexpected failure — delete idempotency key so client can retry
            redisTemplate.delete(idempotencyKey);
            log.error("Order creation failed for requestId: {}", request.getRequestId(), e);
            throw e;

        } finally {
            // Always release lock
            redisLockService.releaseLock(lockKey);
        }
    }
}