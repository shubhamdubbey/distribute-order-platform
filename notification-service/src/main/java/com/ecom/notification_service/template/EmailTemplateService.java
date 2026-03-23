package com.ecom.notification_service.template;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailTemplateService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public String buildOrderPlacedEmail(String orderId, String productId,
                                         int quantity, BigDecimal amount) {
        String time = LocalDateTime.now().format(FORMATTER);
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 30px auto; background: #ffffff;
                                     border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background-color: #4CAF50; padding: 24px; text-align: center; }
                        .header h1 { color: white; margin: 0; font-size: 24px; }
                        .body { padding: 32px; }
                        .body p { color: #333; font-size: 15px; line-height: 1.6; }
                        .order-box { background: #f9f9f9; border-left: 4px solid #4CAF50;
                                     padding: 16px; margin: 20px 0; border-radius: 4px; }
                        .order-box p { margin: 6px 0; font-size: 14px; color: #555; }
                        .order-box span { font-weight: bold; color: #222; }
                        .footer { background: #f4f4f4; padding: 16px; text-align: center; }
                        .footer p { font-size: 12px; color: #999; margin: 0; }
                    </style>
                </head>
                <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ Order Confirmed!</h1>
                    </div>
                    <div class="body">
                        <p>Thank you for your order. We've received it and it's being processed.</p>
                        <div class="order-box">
                            <p>Order ID: <span>%s</span></p>
                            <p>Product ID: <span>%s</span></p>
                            <p>Quantity: <span>%d</span></p>
                            <p>Amount: <span>$%.2f</span></p>
                            <p>Placed At: <span>%s</span></p>
                        </div>
                        <p>We'll notify you once payment is confirmed and your order is dispatched.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Order Platform. All rights reserved.</p>
                    </div>
                </div>
                </body>
                </html>
                """.formatted(orderId, productId, quantity, amount, time);
    }

    public String buildPaymentFailedEmail(String orderId, String productId,
                                           int quantity, BigDecimal amount) {
        String time = LocalDateTime.now().format(FORMATTER);
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 30px auto; background: #ffffff;
                                     border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background-color: #e53935; padding: 24px; text-align: center; }
                        .header h1 { color: white; margin: 0; font-size: 24px; }
                        .body { padding: 32px; }
                        .body p { color: #333; font-size: 15px; line-height: 1.6; }
                        .order-box { background: #fff5f5; border-left: 4px solid #e53935;
                                     padding: 16px; margin: 20px 0; border-radius: 4px; }
                        .order-box p { margin: 6px 0; font-size: 14px; color: #555; }
                        .order-box span { font-weight: bold; color: #222; }
                        .footer { background: #f4f4f4; padding: 16px; text-align: center; }
                        .footer p { font-size: 12px; color: #999; margin: 0; }
                    </style>
                </head>
                <body>
                <div class="container">
                    <div class="header">
                        <h1>❌ Payment Failed</h1>
                    </div>
                    <div class="body">
                        <p>Unfortunately, your payment could not be processed. Your order has been cancelled
                           and inventory has been released.</p>
                        <div class="order-box">
                            <p>Order ID: <span>%s</span></p>
                            <p>Product ID: <span>%s</span></p>
                            <p>Quantity: <span>%d</span></p>
                            <p>Amount: <span>$%.2f</span></p>
                            <p>Failed At: <span>%s</span></p>
                        </div>
                        <p>Please try placing your order again. If the issue persists, contact support.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Order Platform. All rights reserved.</p>
                    </div>
                </div>
                </body>
                </html>
                """.formatted(orderId, productId, quantity, amount, time);
    }
}