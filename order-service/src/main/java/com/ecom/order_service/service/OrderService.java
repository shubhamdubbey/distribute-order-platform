package com.ecom.order_service.service;

import com.ecom.common_lib.constants.RedisKeys;
import com.ecom.common_lib.events.OrderCreatedEvent;
import com.ecom.order_service.dto.CreateOrderRequest;
import com.ecom.order_service.dto.CreateOrderResponse;
import com.ecom.order_service.entity.Order;
import com.ecom.order_service.entity.OrderStatus;
import com.ecom.order_service.event.OrderEventPublisher;
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
    private final OrderEventPublisher orderEventPublisher;

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

        String lockKey = RedisKeys.orderLockKey(request.getUserId().toString());
        boolean lockAcquired = redisLockService.acquireLock(lockKey);

        if (!lockAcquired) {
            redisTemplate.delete(idempotencyKey);
            throw new OrderLockException("Another order is being processed for this user. "
                    + "Please try again.");
        }

        Order savedOrder = null;
        boolean stockDecremented = false;

        try {
            // Step 2: Check and decrement inventory atomically
            stockDecremented = inventoryRedisService.decrementStock(
                    request.getProductId().toString(),
                    request.getQuantity()
            );

            if (!stockDecremented) {
                redisTemplate.delete(idempotencyKey);
                throw new InsufficientStockException("Insufficient stock for product: "
                        + request.getProductId());
            }

            // Step 3: Save order to DB
            Order order = Order.builder()
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .status(OrderStatus.PENDING)
                    .build();

            savedOrder = orderRepository.save(order);
            log.info("Order saved to DB. orderId: {}", savedOrder.getId());

            // Step 4: Publish event to SNS
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(savedOrder.getId())
                    .userId(savedOrder.getUserId())
                    .productId(savedOrder.getProductId())
                    .quantity(savedOrder.getQuantity())
                    .status(savedOrder.getStatus().name())
                    .createdAt(savedOrder.getCreatedAt())
                    .build();

            orderEventPublisher.publishOrderCreated(event);

            // Step 5: Only update idempotency key after everything succeeds
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
            throw e;

        } catch (Exception e) {
            log.error("Order creation failed for requestId: {}. Rolling back.",
                    request.getRequestId(), e);

            // Compensating transaction — undo everything
            // Restore Redis inventory if it was decremented
            if (stockDecremented) {
                inventoryRedisService.restoreStock(
                        request.getProductId().toString(),
                        request.getQuantity()
                );
                log.info("Inventory restored for productId: {}", request.getProductId());
            }

            // Delete orphaned DB order if it was saved
            if (savedOrder != null) {
                orderRepository.delete(savedOrder);
                log.info("Orphaned order deleted. orderId: {}", savedOrder.getId());
            }

            // Delete idempotency key so client can retry
            redisTemplate.delete(idempotencyKey);

            throw e;

        } finally {
            redisLockService.releaseLock(lockKey);
        }
    }
}