package com.nexusmarket.notification;

import com.nexusmarket.notification.event.OrderConfirmedEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.KafkaListener;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

    @KafkaListener(topics = "order-confirmed", groupId = "notification-group")
    public void handleNotification(OrderConfirmedEvent event) {
        log.info("📧 Mock Email dispatched to {}: Your order {} has been successfully confirmed and payment is processed!", event.email(), event.orderNumber());
    }
}
