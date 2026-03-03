package com.ecom.inventory_service.controller;

import com.ecom.inventory_service.config.DlqReprocessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
public class DlqController {

    private final DlqReprocessorService dlqReprocessorService;

    @PostMapping("/reprocess/inventory")
    public ResponseEntity<Map<String, Integer>> reprocessInventory() {
        int count = dlqReprocessorService.reprocessInventoryDlq();
        return ResponseEntity.ok(Map.of("reprocessedCount", count));
    }

    @PostMapping("/reprocess/inventory-payment")
    public ResponseEntity<Map<String, Integer>> reprocessInventoryPayment() {
        int count = dlqReprocessorService.reprocessInventoryPaymentDlq();
        return ResponseEntity.ok(Map.of("reprocessedCount", count));
    }
}