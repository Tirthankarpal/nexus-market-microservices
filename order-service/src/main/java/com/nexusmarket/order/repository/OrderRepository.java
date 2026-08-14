package com.nexusmarket.order.repository;

import com.nexusmarket.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    org.springframework.data.domain.Page<Order> findByUserEmail(String userEmail, org.springframework.data.domain.Pageable pageable);
    Order findByOrderNumber(String orderNumber);
}
