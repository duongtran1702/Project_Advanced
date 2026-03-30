package com.atmin.saber.presentation;

import com.atmin.saber.controller.BookingController;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.enums.OrderStatus;
import com.atmin.saber.service.OrderService;
import com.atmin.saber.util.DBConnection;
import com.atmin.saber.util.SessionContext;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Staff console menu:
 * - View pending bookings (PENDING)
 * - View pending F&B orders (PENDING / PREPARING)
 * - Advance order status: PENDING -> PREPARING -> SERVED
 */
public class StaffMenu {
    private final BookingController bookingController;
    private final OrderService orderService;
    private final Scanner scanner;

    public StaffMenu(BookingController bookingController, OrderService orderService, Scanner scanner) {
        this.bookingController = bookingController;
        this.orderService = orderService;
        this.scanner = scanner;
    }

    public static StaffMenu createDefault(Scanner scanner) {
        // Reuse BookingController default wiring
        BookingController bookingController = BookingController.createDefault();

        // OrderService is hidden inside BookingController, so create a parallel one (same DB)
        DBConnection db = DBConnection.getInstance();
        com.atmin.saber.service.ProductService productService = new com.atmin.saber.service.impl.ProductServiceImpl(
                new com.atmin.saber.dao.impl.ProductDaoImpl(db)
        );
        OrderService orderService = new com.atmin.saber.service.impl.OrderServiceImpl(
                new com.atmin.saber.dao.impl.OrderDaoImpl(db),
                new com.atmin.saber.dao.impl.OrderDetailDaoImpl(db),
                productService
        );

        return new StaffMenu(bookingController, orderService, scanner);
    }

    public void showMenu() {
        while (true) {
            System.out.println("\n============================================");
            System.out.println("                STAFF MENU");
            System.out.println("============================================");
            System.out.println("  1. View pending booking requests");
            System.out.println("  2. View pending F&B orders");
            System.out.println("  3. Update (advance) F&B order status");
            System.out.println("  4. View a customer's latest order status");
            System.out.println("  5. Start a booking (set ACTIVE)");
            System.out.println("  6. Stop a booking (complete & charge wallet)");
            System.out.println("  0. Logout");
            System.out.println("============================================");
            System.out.print("-> Select option (0-6): ");

            if (!scanner.hasNextLine()) {
                SessionContext.clearCurrentRole();
                SessionContext.clearCurrentUser();
                System.out.println("\n[EOF] No more input. Logout...");
                return;
            }
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> safeRun(this::viewPendingBookings);
                case "2" -> safeRun(this::viewPendingOrders);
                case "3" -> safeRun(this::advanceOrderStatus);
                case "4" -> safeRun(this::viewCustomerLatestOrderStatus);
                case "5" -> safeRun(this::startBooking);
                case "6" -> safeRun(this::stopBooking);
                case "0" -> {
                    SessionContext.clearCurrentRole();
                    SessionContext.clearCurrentUser();
                    return;
                }
                default -> System.out.println("Invalid choice. Please select from 0-6.");
            }
        }
    }

    private void startBooking() {
        System.out.println("\n=== START BOOKING (SET ACTIVE) ===");
        System.out.print("Enter customerId (or 0 to return): ");
        if (!scanner.hasNextLine()) return;
        String customerId = scanner.nextLine().trim();
        if ("0".equals(customerId)) return;
        System.out.print("Enter PC ID (or 0 to return): ");
        int pcId = readIntAllowBack();
        if (pcId == 0) return;
        System.out.print("Enter start time (dd/MM/yyyy HH:mm) (or 0 to return): ");
        if (!scanner.hasNextLine()) return;
        String startTimeStr = scanner.nextLine().trim();
        if ("0".equals(startTimeStr)) return;

        java.time.LocalDateTime startTime;
        try {
            startTime = java.time.LocalDateTime.parse(startTimeStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (java.time.format.DateTimeParseException e) {
            System.out.println("Invalid time format.");
            pause();
            return;
        }

        bookingController.startBooking(customerId, pcId, startTime);
        System.out.println("[OK] Booking started.");
        pause();
    }

    private void stopBooking() {
        System.out.println("\n=== STOP BOOKING (COMPLETE & CHARGE) ===");
        System.out.print("Enter customerId (or 0 to return): ");
        if (!scanner.hasNextLine()) return;
        String customerId = scanner.nextLine().trim();
        if ("0".equals(customerId)) return;

        java.util.Optional<java.math.BigDecimal> charged = bookingController.stopActiveBookingAndCharge(customerId);
        if (charged.isEmpty()) {
            System.out.println("No ACTIVE booking found for this customer.");
        } else {
            System.out.println("[OK] Booking completed. Charged amount: " + charged.get());
        }
        pause();
    }

    private int readIntAllowBack() {
        while (true) {
            if (!scanner.hasNextLine()) return 0;
            String raw = scanner.nextLine().trim();
            if (raw.equals("0")) return 0;
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.print("Enter a number (or 0 to return): ");
            }
        }
    }

    private void viewPendingBookings() {
        System.out.println("\n=== PENDING BOOKING REQUESTS ===");
        List<Booking> pending = bookingController.getPendingBookings();
        if (pending.isEmpty()) {
            System.out.println("No pending booking requests.");
            pause();
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        System.out.println("+------------+------+----------------------+----------------------+----------+");
        System.out.printf("| %-10s | %-4s | %-20s | %-20s | %-8s |%n",
                "CUSTOMER", "PC", "START", "END", "STATUS");
        System.out.println("+------------+------+----------------------+----------------------+----------+");
        for (Booking b : pending) {
            System.out.printf("| %-10s | %-4d | %-20s | %-20s | %-8s |%n",
                    shortId(b.getCustomerId()),
                    b.getPcId(),
                    b.getStartTime().format(fmt),
                    b.getExpectedEndTime().format(fmt),
                    b.getStatus().name());
        }
        System.out.println("+------------+------+----------------------+----------------------+----------+");
        pause();
    }

    private void viewPendingOrders() {
        System.out.println("\n=== PENDING F&B ORDERS ===");
        List<Order> orders = orderService.getPendingOrdersForStaff();
        if (orders.isEmpty()) {
            System.out.println("No pending orders.");
            pause();
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        System.out.println("+--------------------------------------+------------+----------------------+-----------+---------------+");
        System.out.printf("| %-36s | %-10s | %-20s | %-9s | %-13s |%n",
                "ORDER ID", "CUSTOMER", "ORDER TIME", "STATUS", "TOTAL");
        System.out.println("+--------------------------------------+------------+----------------------+-----------+---------------+");
        for (Order o : orders) {
            System.out.printf("| %-36s | %-10s | %-20s | %-9s | %-13s |%n",
                    o.getOrderId(),
                    shortId(o.getCustomerId()),
                    o.getOrderTime() == null ? "" : o.getOrderTime().format(fmt),
                    o.getStatus() == null ? "" : o.getStatus().name(),
                    o.getTotalAmount());
        }
        System.out.println("+--------------------------------------+------------+----------------------+-----------+---------------+");
        pause();
    }

    private void advanceOrderStatus() {
        System.out.println("\n=== ADVANCE ORDER STATUS ===");
        System.out.print("Enter orderId (or 0 to return): ");
        if (!scanner.hasNextLine()) return;
        String orderId = scanner.nextLine().trim();
        if ("0".equals(orderId)) return;

        OrderStatus next = orderService.advanceOrderStatusForStaff(orderId);
        System.out.println("[OK] Updated status to: " + next.name() + formatStaffMeaning(next));
        pause();
    }

    private void viewCustomerLatestOrderStatus() {
        System.out.println("\n=== CUSTOMER LATEST ORDER STATUS ===");
        System.out.print("Enter customerId (or 0 to return): ");
        if (!scanner.hasNextLine()) return;
        String customerId = scanner.nextLine().trim();
        if ("0".equals(customerId)) return;

        Optional<Order> latest = orderService.getLatestOrderOfCustomer(customerId.trim());
        if (latest.isEmpty()) {
            System.out.println("You have no orders yet.");
            pause();
            return;
        }

        Order o = latest.get();
        System.out.println("\nYour latest order:");
        System.out.println("- OrderId: " + o.getOrderId());
        System.out.println("- Status : " + (o.getStatus() == null ? "" : o.getStatus().name()) + formatStaffMeaning(o.getStatus()));
        System.out.println("- Total  : " + o.getTotalAmount());
        pause();
    }

    private static String formatStaffMeaning(OrderStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> " (Confirmed)";
            case PREPARING -> " (Serving)";
            case SERVED -> " (Completed)";
            case PAID -> " (Paid)";
        };
    }

    private static String shortId(String id) {
        if (id == null) return "";
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private void safeRun(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
            pause();
        }
    }

    private void pause() {
        System.out.print("\nPress Enter to return to menu...");
        if (!scanner.hasNextLine()) return;
        try {
            scanner.nextLine();
        } catch (Exception ignored) {
        }
    }
}
