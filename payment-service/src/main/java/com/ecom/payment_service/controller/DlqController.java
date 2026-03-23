package com.ecom.payment_service.controller;

import com.ecom.payment_service.config.DlqMonitorService;
import com.ecom.payment_service.config.DlqReprocessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
@Slf4j
public class DlqController {

    private final DlqMonitorService dlqMonitorService;
    private final DlqReprocessorService dlqReprocessorService;

    @Value("${cloud.aws.sqs.payment-queue-url}")
    private String orderUpdatesQueueUrl;

    @GetMapping("/monitor")
    public ResponseEntity<String> monitor() {
        dlqMonitorService.monitorDlq();
        return ResponseEntity.ok("DLQ check triggered");
    }

    @PostMapping("/reprocess")
    public ResponseEntity<Map<String, Integer>> reprocess() {
        int count = dlqReprocessorService.reprocessMessages(orderUpdatesQueueUrl);
        return ResponseEntity.ok(Map.of("reprocessedCount", count));
    }
}