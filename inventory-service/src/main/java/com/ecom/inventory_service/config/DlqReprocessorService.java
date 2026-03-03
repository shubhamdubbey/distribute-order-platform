package com.ecom.inventory_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqReprocessorService {

    private final SqsClient sqsClient;

    @Value("${cloud.aws.sqs.inventory-dlq-url}")
    private String inventoryDlqUrl;

    @Value("${cloud.aws.sqs.inventory-queue-url}")
    private String inventoryQueueUrl;

    @Value("${cloud.aws.sqs.inventory-payment-dlq-url}")
    private String inventoryPaymentDlqUrl;

    @Value("${cloud.aws.sqs.inventory-payment-queue-url}")
    private String inventoryPaymentQueueUrl;

    public int reprocessInventoryDlq() {
        return reprocess(inventoryDlqUrl, inventoryQueueUrl, "inventory-service-dlq");
    }

    public int reprocessInventoryPaymentDlq() {
        return reprocess(inventoryPaymentDlqUrl, inventoryPaymentQueueUrl,
                "inventory-payment-dlq");
    }

    private int reprocess(String dlqUrl, String targetQueueUrl, String dlqName) {
        int reprocessedCount = 0;

        try {
            while (true) {
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(dlqUrl)
                        .maxNumberOfMessages(10)
                        .visibilityTimeout(30)
                        .messageAttributeNames("All")
                        .build();

                List<Message> messages = sqsClient
                        .receiveMessage(receiveRequest)
                        .messages();

                if (messages.isEmpty()) break;

                for (Message message : messages) {
                    try {
                        // Send back to original queue
                        sqsClient.sendMessage(SendMessageRequest.builder()
                                .queueUrl(targetQueueUrl)
                                .messageBody(message.body())
                                .build());

                        // Delete from DLQ only after successful send
                        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                                .queueUrl(dlqUrl)
                                .receiptHandle(message.receiptHandle())
                                .build());

                        reprocessedCount++;
                        log.info("Reprocessed message {} from {}",
                                message.messageId(), dlqName);

                    } catch (Exception e) {
                        log.error("Failed to reprocess message {} from {}",
                                message.messageId(), dlqName, e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("DLQ reprocessing failed for {}", dlqName, e);
        }

        log.info("Reprocessed {} messages from {}", reprocessedCount, dlqName);
        return reprocessedCount;
    }
}