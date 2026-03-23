package com.ecom.payment_service.service;

import com.ecom.common_lib.events.InventoryReservedEvent;
import com.ecom.common_lib.events.PaymentFailedEvent;
import com.ecom.common_lib.events.PaymentSuccessEvent;
import com.ecom.payment_service.entity.Payment;
import com.ecom.payment_service.entity.PaymentStatus;
import com.ecom.payment_service.event.PaymentEventPublisher;
import com.ecom.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final Random random = new Random();

    @Transactional
    public void processPayment(InventoryReservedEvent event, String correlationId) {

        log.info("Processing payment for orderId: {}, amount: {}",
                event.getOrderId(), event.getAmount());

        // Step 1: Check for duplicate payment
        if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.warn("Payment already exists for orderId: {}. Skipping.",
                    event.getOrderId());
            return;
        }

        // Step 2: Save payment record as PROCESSING
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .status(PaymentStatus.PROCESSING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment record created with status PROCESSING for orderId: {}",
                event.getOrderId());

        try {
            // Step 3: Simulate payment gateway delay
            Thread.sleep(1000);

            // Step 4: Simulate 80% success rate
            int outcome = random.nextInt(10) + 1;
            boolean paymentSuccess = outcome > 2;

            if (paymentSuccess) {
                handleSuccess(payment, event, correlationId);
            } else {
                handleFailure(payment, event, correlationId, "Payment declined by gateway");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted for orderId: {}", event.getOrderId());
            handleFailure(payment, event, correlationId, "Payment processing interrupted");
        }
    }

    private void handleSuccess(Payment payment,
                               InventoryReservedEvent event,
                               String correlationId) {

        // Update payment status
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        log.info("Payment SUCCESS for orderId: {}, amount: {}",
                event.getOrderId(), event.getAmount());

        // Publish success event
        PaymentSuccessEvent successEvent = PaymentSuccessEvent.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .amount(event.getAmount())
                .correlationId(correlationId)
                .processedAt(Instant.now())
                .build();

        eventPublisher.publishPaymentSuccess(successEvent);
    }

    private void handleFailure(Payment payment,
                               InventoryReservedEvent event,
                               String correlationId,
                               String reason) {

        // Update payment status
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        log.warn("Payment FAILED for orderId: {}. Reason: {}",
                event.getOrderId(), reason);

        // Publish failed event
        PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .reason(reason)
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .correlationId(correlationId)
                .failedAt(Instant.now())
                .amount(event.getAmount())
                .build();

        eventPublisher.publishPaymentFailed(failedEvent);
    }
}