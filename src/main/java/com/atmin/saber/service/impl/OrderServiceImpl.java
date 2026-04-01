package com.atmin.saber.service.impl;

import com.atmin.saber.dao.OrderDetailDao;
import com.atmin.saber.dao.OrderDao;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.model.Product;
import com.atmin.saber.model.enums.OrderStatus;
import com.atmin.saber.model.enums.TransactionType;
import com.atmin.saber.service.OrderService;
import com.atmin.saber.service.ProductService;
import com.atmin.saber.service.WalletService;
import com.atmin.saber.util.DBConnection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.SQLException;

public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;
    private final OrderDetailDao orderDetailDao;
    private final ProductService productService;
    private final WalletService walletService;
    private final DBConnection db;

    public OrderServiceImpl(OrderDao orderDao, OrderDetailDao orderDetailDao, ProductService productService, WalletService walletService) {
        this.orderDao = orderDao;
        this.orderDetailDao = orderDetailDao;
        this.productService = productService;
        this.walletService = walletService;
        this.db = DBConnection.getInstance();
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

                // 1) Build order details & total.
                // IMPORTANT: Do NOT decrease stock here.
                // Stock will be deducted when staff accepts/advances the order.
                for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                    int productId = entry.getKey();
                    int qty = entry.getValue() == null ? 0 : entry.getValue();
                    if (qty <= 0) continue;

                    Product p = productService.getById(productId)
                            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

                    BigDecimal unitPrice = p.getPrice() == null ? BigDecimal.ZERO : p.getPrice();
                    total = total.add(unitPrice.multiply(BigDecimal.valueOf(qty)));

                    OrderDetail d = new OrderDetail();
                    d.setOrderId(-1);
                    d.setId(productId);
                    d.setQuantity(qty);
                    d.setUnitPrice(unitPrice);
                    details.add(d);
                }

                // 2) Insert order + details (still in a transaction).
                Order order = new Order();
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
    public OrderStatus advanceOrderStatusForStaff(int orderId) {
        // We don't have a findById yet; re-use list and locate by id.
        // (keeps change small and compatible with existing DAO)
        Order current = orderDao.findPendingForStaff().stream()
                .filter(o -> orderId == o.getOrderId())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order not found or not updatable: " + orderId));

        OrderStatus next = current.getStatus().nextForStaff();

        // Staff ACCEPTS the order when moving from PENDING -> COMPLETED.
        // At acceptance time, we must deduct inventory. If out of stock, we reject + refund.
        if (current.getStatus() == OrderStatus.PENDING && next == OrderStatus.COMPLETED) {
            if (orderDetailDao == null || walletService == null) {
                throw new IllegalStateException("OrderDetailDao/WalletService is not configured");
            }

            try (Connection con = db.getConnection()) {
                boolean oldAutoCommit = con.getAutoCommit();
                con.setAutoCommit(false);
                try {
                    // 1) Check & decrease stock for all items
                    List<OrderDetail> items = orderDetailDao.findByOrderId(con, orderId);
                    if (items.isEmpty()) {
                        throw new RuntimeException("No items found for order: " + orderId + ". Order details may not have been saved correctly.");
                    }
                    for (OrderDetail d : items) {
                        int pid = d.getId();
                        int qty = d.getQuantity() == null ? 0 : d.getQuantity();
                        if (qty <= 0) continue;

                        Product p = productService.getById(pid)
                                .orElseThrow(() -> new RuntimeException("Product not found: " + pid));

                        boolean okStock = productService.decreaseStockIfEnough(con, pid, qty);
                        if (!okStock) {
                            // Out of stock: cancel + refund in the same transaction
                            boolean st = orderDao.updateStatus(orderId, OrderStatus.CANCELLED.name());
                            if (!st) throw new RuntimeException("Failed to cancel order after out-of-stock");

                            // Add refund to wallet balance
                            String addBalSql = "UPDATE users SET balance = balance + ? WHERE user_id = ?";
                            try (java.sql.PreparedStatement balPs = con.prepareStatement(addBalSql)) {
                                balPs.setBigDecimal(1, current.getTotalAmount());
                                balPs.setString(2, current.getCustomerId());
                                int okUser = balPs.executeUpdate();
                                if (okUser <= 0) throw new RuntimeException("Failed to refund wallet balance");
                            }

                            // Insert REFUND transaction (positive amount - adds back to balance)
                            String insertTxSql = "INSERT INTO transactions(user_id, amount, transaction_type, description, created_at) VALUES(?, ?, ?, ?, ?)";
                            try (java.sql.PreparedStatement txPs = con.prepareStatement(insertTxSql)) {
                                txPs.setString(1, current.getCustomerId());
                                txPs.setBigDecimal(2, current.getTotalAmount());
                                txPs.setString(3, TransactionType.REFUND.name());
                                txPs.setString(4, "Refund for out-of-stock order: " + orderId + " (" + p.getProductName() + ")");
                                txPs.setTimestamp(5, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                                txPs.executeUpdate();
                            }

                            con.commit();
                            return OrderStatus.CANCELLED;
                        }
                    }

                    // 2) If stock ok: advance status
                    boolean ok = orderDao.updateStatus(orderId, next.name());
                    if (!ok) throw new RuntimeException("Failed to update order status");
                    con.commit();
                    return next;
                } catch (RuntimeException ex) {
                    con.rollback();
                    throw ex;
                } catch (SQLException ex) {
                    con.rollback();
                    throw new RuntimeException("Failed to accept order: " + ex.getMessage(), ex);
                } finally {
                    con.setAutoCommit(oldAutoCommit);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to accept order: " + e.getMessage(), e);
            }
        }

        boolean ok = orderDao.updateStatus(orderId, next.name());
        if (!ok) throw new RuntimeException("Failed to update order status");
        return next;
    }

    @Override
    public void rejectOrderAndRefund(int orderId) {
        // Must be PENDING to be rejected by staff
        Order current = orderDao.findPendingForStaff().stream()
                .filter(o -> orderId == o.getOrderId())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order not found or not updatable: " + orderId));

        try (Connection con = db.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                // IMPORTANT: keep atomicity by using the SAME connection for both steps.
                if (walletService == null) {
                    throw new IllegalStateException("WalletService is not configured");
                }

                // 1) Update order status to CANCEL (with this transaction)
                // (OrderDao.updateStatus currently opens its own connection, so do inline SQL here)
                String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
                try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, OrderStatus.CANCELLED.name());
                    ps.setInt(2, orderId);
                    int updated = ps.executeUpdate();
                    if (updated <= 0) throw new RuntimeException("Failed to update order status to CANCELLED");
                }

                // 2) Refund to wallet (with this transaction)
                // (WalletService.topUp opens its own connection, so do inline SQL + tx insert here)
                String userId = current.getCustomerId();
                BigDecimal amount = current.getTotalAmount() == null ? BigDecimal.ZERO : current.getTotalAmount();
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    String addBalSql = "UPDATE users SET balance = balance + ? WHERE user_id = ?";
                    try (java.sql.PreparedStatement ps = con.prepareStatement(addBalSql)) {
                        ps.setBigDecimal(1, amount);
                        ps.setString(2, userId);
                        int okUser = ps.executeUpdate();
                        if (okUser <= 0) throw new RuntimeException("Failed to refund wallet balance");
                    }

                    String insertTxSql = "INSERT INTO transactions(user_id, amount, transaction_type, description, created_at) VALUES(?, ?, ?, ?, ?)";
                    try (java.sql.PreparedStatement ps = con.prepareStatement(insertTxSql)) {
                        ps.setString(1, userId);
                        ps.setBigDecimal(2, amount);
                        ps.setString(3, TransactionType.REFUND.name());
                        ps.setString(4, "Refund for rejected order: " + orderId);
                        ps.setTimestamp(5, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        ps.executeUpdate();
                    }
                }

                con.commit();
            } catch (RuntimeException ex) {
                con.rollback();
                throw ex;
            } catch (SQLException ex) {
                con.rollback();
                throw new RuntimeException("Failed to reject order and refund: " + ex.getMessage(), ex);
            } finally {
                con.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reject order and refund: " + e.getMessage(), e);
        }
    }

    @Override
    public int advanceAllPendingOrdersForStaff() {
        List<Order> pending = orderDao.findPendingForStaff(); // already FIFO by ORDER BY order_time ASC
        int updated = 0;
        for (Order o : pending) {
            if (o == null) continue;
            if (o.getStatus() == null || !o.getStatus().isStaffUpdatable()) continue;

            try {
                // Call advanceOrderStatusForStaff to properly handle stock checking
                OrderStatus result = advanceOrderStatusForStaff(o.getOrderId());
                if (result != OrderStatus.CANCELLED) {
                    updated++;
                }
            } catch (Exception ex) {
                // Log error but continue with next order
                System.err.println("ERROR: Failed to advance order " + o.getOrderId() + ": " + ex.getMessage());
            }
        }
        return updated;
    }

    @Override
    public List<Order> getAllOrdersOfCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) return List.of();
        return orderDao.findAllByCustomerId(customerId);
    }

    @Override
    public List<OrderDetail> getOrderDetails(int orderId) {
        if (orderDetailDao == null) {
            throw new IllegalStateException("OrderDetailDao is not configured");
        }
        return orderDetailDao.findByOrderId(orderId);
    }
}

