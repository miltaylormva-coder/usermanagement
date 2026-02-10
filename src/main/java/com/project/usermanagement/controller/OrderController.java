package com.project.usermanagement.controller;

import com.project.usermanagement.dto.request.CreateOrderRequest;
import com.project.usermanagement.dto.request.UpdateOrderRequest;
import com.project.usermanagement.dto.response.ApiResponse;
import com.project.usermanagement.dto.response.OrderResponse;
import com.project.usermanagement.enums.OrderStatus;
import com.project.usermanagement.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order management
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create new order
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        logger.info("Create order request received");
        OrderResponse orderResponse = orderService.createOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Order created successfully", orderResponse));
    }

    /**
     * Get order by ID
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        logger.debug("Get order request for ID: {}", id);

        OrderResponse orderResponse = orderService.getOrderById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Order retrieved successfully", orderResponse));
    }

    /**
     * Get current user's orders
     * GET /api/orders/my-orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        logger.debug("Get my orders request");

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderResponse> orders = orderService.getMyOrders(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * Get all orders (Admin only)
     * GET /api/orders
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        logger.debug("Get all orders request (Admin)");

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderResponse> orders = orderService.getAllOrders(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("All orders retrieved successfully", orders));
    }

    /**
     * Get orders by status (Admin only)
     * GET /api/orders/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("Get orders by status: {}", status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * Search orders (Admin only)
     * GET /api/orders/search
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> searchOrders(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("Search orders with query: {}", query);

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponse> orders = orderService.searchOrders(query, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Search results retrieved successfully", orders));
    }

    /**
     * Update order
     * PUT /api/orders/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request) {

        logger.info("Update order request for ID: {}", id);
        OrderResponse orderResponse = orderService.updateOrder(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Order updated successfully", orderResponse));
    }

    /**
     * Update order status (Admin only)
     * PATCH /api/orders/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {

        logger.info("Update order status request for ID: {} to {}", id, status);
        OrderResponse orderResponse = orderService.updateOrderStatus(id, status);

        return ResponseEntity.ok(
                ApiResponse.success("Order status updated successfully", orderResponse));
    }

    /**
     * Cancel order
     * DELETE /api/orders/{id}/cancel
     */
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        logger.info("Cancel order request for ID: {}", id);

        orderService.cancelOrder(id);

        return ResponseEntity.ok(
                ApiResponse.success("Order cancelled successfully"));
    }

    /**
     * Delete order (Admin only)
     * DELETE /api/orders/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        logger.warn("Delete order request for ID: {}", id);

        orderService.deleteOrder(id);

        return ResponseEntity.ok(
                ApiResponse.success("Order deleted successfully"));
    }
}