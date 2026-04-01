package com.atmin.saber.service.impl;

import com.atmin.saber.dao.OrderDetailDao;
import com.atmin.saber.dao.OrderDao;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.model.Product;
import com.atmin.saber.model.enums.OrderStatus;
import com.atmin.saber.service.OrderService;
import com.atmin.saber.service.ProductService;
import com.atmin.saber.util.DBConnection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;
    private final OrderDetailDao orderDetailDao;
    private final ProductService productService;
    private final DBConnection db;

    public OrderServiceImpl(OrderDao orderDao, OrderDetailDao orderDetailDao, ProductService productService) {
        this.orderDao = orderDao;
        this.orderDetailDao = orderDetailDao;
        this.productService = productService;
        this.db = DBConnection.getInstance();
    }

    /**
     * Backward-compatible constructor (details view will not be available if DAO is missing).
     */
    public OrderServiceImpl(OrderDao orderDao, ProductService productService) {
        this(orderDao, null, productService);
    }

    @Override
    public CreatedOrder createOrderForBooking(String customerId, Booking booking, Map<Integer, Integer> items) {
        if (booking == null) throw new IllegalArgumentException("Booking is required");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Order items is empty");

        try (Connection con = db.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                BigDecimal total = BigDecimal.ZERO;
                List<OrderDetail> details = new ArrayList<>();

                // 1) decrease stock for all items (or rollback)
                for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                    int productId = entry.getKey();
                    int qty = entry.getValue() == null ? 0 : entry.getValue();
                    if (qty <= 0) continue;

                    Product p = productService.getById(productId)
                            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

                    boolean ok = productService.decreaseStockIfEnough(con, productId, qty);
                    if (!ok) {
                        throw new RuntimeException("Not enough stock for product: " + p.getProductName());
                    }

                    BigDecimal unitPrice = p.getPrice() == null ? BigDecimal.ZERO : p.getPrice();
                    total = total.add(unitPrice.multiply(BigDecimal.valueOf(qty)));

                    OrderDetail d = new OrderDetail();
                    d.setOrderId(null);
                    d.setId(productId);
                    d.setQuantity(qty);
                    d.setUnitPrice(unitPrice);
                    details.add(d);
                }

                // 2) insert order + details
                Order order = new Order();
                order.setOrderId(UUID.randomUUID().toString());
                order.setCustomerId(customerId);
                order.setOrderTime(LocalDateTime.now());
                order.setStatus(OrderStatus.PENDING);
                order.setTotalAmount(total);

                order.setBookingCustomerId(booking.getCustomerId());
                order.setBookingPcId(booking.getPcId());
                order.setBookingStartTime(booking.getStartTime());

                orderDao.insert(con, order);
                orderDao.insertDetails(con, order.getOrderId(), details);

                con.commit();
                return new CreatedOrder(order, details);
            } catch (RuntimeException ex) {
                con.rollback();
                throw ex;
            } catch (SQLException ex) {
                con.rollback();
                throw new RuntimeException("Failed to create order: " + ex.getMessage(), ex);
            } finally {
                con.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> getPendingOrdersForStaff() {
        return orderDao.findPendingForStaff();
    }

    @Override
    public OrderStatus advanceOrderStatusForStaff(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }

        // We don't have a findById yet; re-use list and locate by id.
        // (keeps change small and compatible with existing DAO)
        Order current = orderDao.findPendingForStaff().stream()
                .filter(o -> orderId.equals(o.getOrderId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order not found or not updatable: " + orderId));

        OrderStatus next = current.getStatus().nextForStaff();
        boolean ok = orderDao.updateStatus(orderId, next.name());
        if (!ok) throw new RuntimeException("Failed to update order status");
        return next;
    }

    @Override
    public int advanceAllPendingOrdersForStaff() {
        List<Order> pending = orderDao.findPendingForStaff(); // already FIFO by ORDER BY order_time ASC
        int updated = 0;
        for (Order o : pending) {
            if (o == null || o.getOrderId() == null || o.getOrderId().isBlank()) continue;
            if (o.getStatus() == null || !o.getStatus().isStaffUpdatable()) continue;

            OrderStatus next = o.getStatus().nextForStaff();
            boolean ok = orderDao.updateStatus(o.getOrderId(), next.name());
            if (ok) updated++;
        }
        return updated;
    }

    @Override
    public Optional<Order> getLatestOrderOfCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) return Optional.empty();
        return orderDao.findLatestByCustomerId(customerId);
    }

    @Override
    public List<Order> getAllOrdersOfCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) return List.of();
        return orderDao.findAllByCustomerId(customerId);
    }

    @Override
    public List<OrderDetail> getOrderDetails(String orderId) {
        if (orderId == null || orderId.isBlank()) return List.of();
        if (orderDetailDao == null) {
            throw new IllegalStateException("OrderDetailDao is not configured");
        }
        return orderDetailDao.findByOrderId(orderId);
    }
}

