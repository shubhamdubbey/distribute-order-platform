package com.ecom.order_service.controller;

import com.ecom.order_service.dto.CreateOrderRequest;
import com.ecom.order_service.dto.CreateOrderResponse;
import com.ecom.order_service.service.InventoryRedisService;
import com.ecom.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    private final InventoryRedisService inventoryRedisService;

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<Long> getStock(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryRedisService.getStock(productId));
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestBody CreateOrderRequest request) {

        log.info("Received order request with requestId: {}", request.getRequestId());
        CreateOrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(response);
    }
}