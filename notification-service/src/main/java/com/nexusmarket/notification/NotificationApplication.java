package com.nexusmarket.notification;

import com.nexusmarket.notification.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.KafkaListener;

@SpringBootApplication
@EnableDiscoveryClient
public class NotificationApplication {

    private static final Logger log = LoggerFactory.getLogger(NotificationApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

    @KafkaListener(topics = "order-placed", groupId = "notification-group")
    public void handleNotification(OrderPlacedEvent event) {
        log.info("📧 Mock Email dispatched to {}: Your order {} has been successfully placed!", event.email(), event.orderNumber());
    }
}
