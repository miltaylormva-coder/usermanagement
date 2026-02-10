package com.project.usermanagement.service;

import com.project.usermanagement.dto.request.CreateOrderRequest;
import com.project.usermanagement.dto.request.UpdateOrderRequest;
import com.project.usermanagement.dto.response.OrderResponse;
import com.project.usermanagement.entity.Order;
import com.project.usermanagement.entity.User;
import com.project.usermanagement.enums.OrderStatus;
import com.project.usermanagement.exception.BadRequestException;
import com.project.usermanagement.exception.ResourceNotFoundException;
import com.project.usermanagement.repository.OrderRepository;
import com.project.usermanagement.repository.UserRepository;
import com.project.usermanagement.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for order management operations
 */
@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new order
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        logger.info("Creating new order");

        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .totalAmount(request.getTotalAmount())
                .deliveryAddress(request.getDeliveryAddress())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully with number: {}", orderNumber);

        return mapToOrderResponse(savedOrder);
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        logger.debug("Fetching order with ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        // Check if user has permission to view this order
        validateOrderAccess(order);

        return mapToOrderResponse(order);
    }

    /**
     * Get all orders for current user
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        Long userId = getCurrentUserId();
        logger.debug("Fetching orders for user ID: {}", userId);

        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToOrderResponse);
    }

    /**
     * Get all orders (Admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        logger.debug("Fetching all orders");
        return orderRepository.findAll(pageable)
                .map(this::mapToOrderResponse);
    }

    /**
     * Get orders by status
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        logger.debug("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status, pageable)
                .map(this::mapToOrderResponse);
    }

    /**
     * Search orders
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchOrders(String searchTerm, Pageable pageable) {
        logger.debug("Searching orders with term: {}", searchTerm);
        return orderRepository.searchOrders(searchTerm, pageable)
                .map(this::mapToOrderResponse);
    }

    /**
     * Update order
     */
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        logger.info("Updating order with ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        // Check if user has permission to update this order
        validateOrderAccess(order);

        // Check if order is modifiable
        if (!order.isModifiable()) {
            throw new BadRequestException("Order cannot be modified in current status: " + order.getStatus());
        }

        // Update fields
        if (request.getTotalAmount() != null) {
            order.setTotalAmount(request.getTotalAmount());
        }
        if (request.getDeliveryAddress() != null) {
            order.setDeliveryAddress(request.getDeliveryAddress());
        }
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
        if (request.getStatus() != null && isAdmin()) {
            order.setStatus(request.getStatus());
        }

        Order updatedOrder = orderRepository.save(order);
        logger.info("Order updated successfully with ID: {}", id);

        return mapToOrderResponse(updatedOrder);
    }

    /**
     * Update order status (Admin only)
     */
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        logger.info("Updating order status for ID: {} to {}", id, status);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        logger.info("Order status updated successfully");
        return mapToOrderResponse(updatedOrder);
    }

    /**
     * Cancel order
     */
    public void cancelOrder(Long id) {
        logger.info("Cancelling order with ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        // Check if user has permission
        validateOrderAccess(order);

        // Check if order can be cancelled
        if (!order.isCancellable()) {
            throw new BadRequestException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        logger.info("Order cancelled successfully with ID: {}", id);
    }

    /**
     * Delete order (Admin only)
     */
    public void deleteOrder(Long id) {
        logger.warn("Deleting order with ID: {}", id);

        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", "id", id);
        }

        orderRepository.deleteById(id);
        logger.warn("Order deleted successfully with ID: {}", id);
    }

    /**
     * Generate unique order number
     */
    private String generateOrderNumber() {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "-" + timestamp.substring(timestamp.length() - 6) + "-" + uuid;
    }

    /**
     * Get current authenticated user ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }

    /**
     * Check if current user is admin
     */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Validate if current user has access to the order
     */
    private void validateOrderAccess(Order order) {
        if (!isAdmin() && !order.getUser().getId().equals(getCurrentUserId())) {
            throw new AccessDeniedException("You don't have permission to access this order");
        }
    }

    /**
     * Map Order entity to OrderResponse DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .username(order.getUser().getUsername())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .deliveryAddress(order.getDeliveryAddress())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}