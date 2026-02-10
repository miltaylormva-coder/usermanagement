package com.project.usermanagement.controller;

import com.project.usermanagement.dto.response.ApiResponse;
import com.project.usermanagement.enums.OrderStatus;
import com.project.usermanagement.repository.OrderRepository;
import com.project.usermanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for admin-specific operations
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public AdminController(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Get system statistics
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        logger.info("Fetching system statistics");

        Map<String, Object> stats = new HashMap<>();

        // User statistics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findByIsActiveTrue(
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getTotalElements();

        Map<String, Long> userStats = new HashMap<>();
        userStats.put("total", totalUsers);
        userStats.put("active", activeUsers);
        userStats.put("inactive", totalUsers - activeUsers);

        // Order statistics
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long processingOrders = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long shippedOrders = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        Map<String, Long> orderStats = new HashMap<>();
        orderStats.put("total", totalOrders);
        orderStats.put("pending", pendingOrders);
        orderStats.put("confirmed", confirmedOrders);
        orderStats.put("processing", processingOrders);
        orderStats.put("shipped", shippedOrders);
        orderStats.put("delivered", deliveredOrders);
        orderStats.put("cancelled", cancelledOrders);

        stats.put("users", userStats);
        stats.put("orders", orderStats);

        logger.info("System statistics retrieved successfully");
        return ResponseEntity.ok(
                ApiResponse.success("Statistics retrieved successfully", stats));
    }

    /**
     * Get dashboard summary
     * GET /api/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        logger.info("Fetching admin dashboard data");

        Map<String, Object> dashboard = new HashMap<>();

        // Quick stats
        dashboard.put("totalUsers", userRepository.count());
        dashboard.put("totalOrders", orderRepository.count());
        dashboard.put("pendingOrders", orderRepository.countByStatus(OrderStatus.PENDING));
        dashboard.put("activeUsers", userRepository.findByIsActiveTrue(
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getTotalElements());

        logger.info("Dashboard data retrieved successfully");
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard data retrieved successfully", dashboard));
    }
}