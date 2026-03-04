package com.ecom.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SesClient sesClient;

    @Value("${notification.ses.sender-email}")
    private String senderEmail;

    @Value("${notification.ses.sender-name}")
    private String senderName;

    public void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(senderName + " <" + senderEmail + ">")
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data(subject)
                                    .charset("UTF-8")
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .data(htmlBody)
                                            .charset("UTF-8")
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("Email sent to {} | Subject: {} | MessageId: {}",
                    toEmail, subject, response.messageId());

        } catch (SesException e) {
            log.error("Failed to send email to {} | Error: {}", toEmail, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Email sending failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}