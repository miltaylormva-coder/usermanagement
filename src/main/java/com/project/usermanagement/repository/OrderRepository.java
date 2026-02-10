package com.project.usermanagement.repository;

import com.project.usermanagement.entity.Order;
import com.project.usermanagement.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order entity
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by order number
     * 
     * @param orderNumber Order number
     * @return Optional containing the order if found
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find all orders for a specific user
     * 
     * @param userId   User ID
     * @param pageable Pagination information
     * @return Page of user's orders
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * Find orders by user ID and status
     * 
     * @param userId   User ID
     * @param status   Order status
     * @param pageable Pagination information
     * @return Page of matching orders
     */
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /**
     * Find orders by status
     * 
     * @param status   Order status
     * @param pageable Pagination information
     * @return Page of orders with the given status
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find orders within a date range
     * 
     * @param startDate Start date
     * @param endDate   End date
     * @param pageable  Pagination information
     * @return Page of orders within the date range
     */
    Page<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Count orders by user ID
     * 
     * @param userId User ID
     * @return Number of orders
     */
    long countByUserId(Long userId);

    /**
     * Count orders by status
     * 
     * @param status Order status
     * @return Number of orders with the given status
     */
    long countByStatus(OrderStatus status);

    /**
     * Search orders by order number or delivery address
     * 
     * @param searchTerm Search term
     * @param pageable   Pagination information
     * @return Page of matching orders
     */
    @Query("SELECT o FROM Order o WHERE " +
            "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Order> searchOrders(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Check if order number exists
     * 
     * @param orderNumber Order number
     * @return true if exists, false otherwise
     */
    boolean existsByOrderNumber(String orderNumber);
}