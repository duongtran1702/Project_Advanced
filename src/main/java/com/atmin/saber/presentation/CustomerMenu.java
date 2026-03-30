package com.atmin.saber.presentation;

import com.atmin.saber.controller.BookingController;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.User;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.service.OrderService;
import com.atmin.saber.service.WalletService;
import com.atmin.saber.model.Transaction;
import com.atmin.saber.util.CyberColors;
import com.atmin.saber.util.DBConnection;
import com.atmin.saber.util.SessionContext;

import java.util.Scanner;

public class CustomerMenu {
    private final BookingController bookingController;
    private final OrderService orderService;
    private final WalletService walletService;
    private final Scanner scanner;
    
    public CustomerMenu(BookingController bookingController, OrderService orderService, WalletService walletService, Scanner scanner) {
        this.bookingController = bookingController;
        this.orderService = orderService;
        this.walletService = walletService;
        this.scanner = scanner;
    }
    
    public static CustomerMenu createDefault(Scanner scanner) {
        DBConnection db = DBConnection.getInstance();
        com.atmin.saber.service.ProductService productService = new com.atmin.saber.service.impl.ProductServiceImpl(
                new com.atmin.saber.dao.impl.ProductDaoImpl(db)
        );
        OrderService orderService = new com.atmin.saber.service.impl.OrderServiceImpl(
                new com.atmin.saber.dao.impl.OrderDaoImpl(db),
                new com.atmin.saber.dao.impl.OrderDetailDaoImpl(db),
                productService
        );

        com.atmin.saber.dao.UserDao userDao = new com.atmin.saber.dao.impl.UserDaoImpl(db);
        com.atmin.saber.dao.TransactionDao txDao = new com.atmin.saber.dao.impl.TransactionDaoImpl(db);
        WalletService walletService = new com.atmin.saber.service.impl.WalletServiceImpl(userDao, txDao, db);

        return new CustomerMenu(BookingController.createDefault(), orderService, walletService, scanner);
    }

    private boolean ensureHasNextLine() {
        return scanner != null && scanner.hasNextLine();
    }
    
    public void showMenu() {
        while (true) {
            showDashboard();
            
            System.out.print(CyberColors.YELLOW + "-> Select option (0-11): " + CyberColors.RESET);
            String choice;
            try {
                if (!ensureHasNextLine()) {
                    // Piped input ended: logout/return to caller instead of spamming prompts
                    SessionContext.clearCurrentRole();
                    SessionContext.clearCurrentUser();
                    System.out.println("\n[EOF] No more input. Logout...");
                    return;
                }
                choice = scanner.nextLine().trim();
            } catch (Exception e) {
                SessionContext.clearCurrentRole();
                SessionContext.clearCurrentUser();
                return;
            }
            
            switch (choice) {
                case "1":
                    safeRun(this::showAvailablePCs);
                    break;
                case "2":
                    safeRun(this::makeBooking);
                    break;
                case "3":
                    safeRun(this::viewBookingHistory);
                    break;
                case "4":
                    safeRun(this::cancelBooking);
                    break;
                case "5":
                    safeRun(this::viewOrderHistory);
                    break;
                case "6":
                    safeRun(this::viewOrderDetailsById);
                    break;
                case "7":
                    safeRun(this::viewBalance);
                    break;
                case "8":
                    safeRun(this::topUp);
                    break;
                case "9":
                    safeRun(this::viewTransactions);
                    break;
                case "10":
                    safeRun(this::viewActiveSession);
                    break;
                case "11":
                    return;
                case "0":
                    System.out.println("Thank you for using our service!");
                    SessionContext.clearCurrentRole();
                    SessionContext.clearCurrentUser();
                    return;
                default:
                    System.out.println("Invalid choice. Please select from 0-11.");
            }
        }
    }

    private void showDashboard() {
        String f = CyberColors.CYAN;
        String r = CyberColors.RESET;
        String b = CyberColors.BOLD;

        System.out.println();
        System.out.println(f + "  ╔══════════════════════════════════════════════════════╗" + r);
        System.out.printf(f + "  ║ %-52s ║%n" + r, b + "                CUSTOMER DASHBOARD" + r);
        System.out.println(f + "  ╠══════════════════════════╦═══════════════════════════╣" + r);

        // Left column (1-5), Right column (6-10) like provided sample
        System.out.printf(f + "  ║ " + r + "%-24s" + f + " ║ " + r + "%-25s" + f + " ║%n" + r,
                "1. View available PCs", "6. Order details");
        System.out.printf(f + "  ║ " + r + "%-24s" + f + " ║ " + r + "%-25s" + f + " ║%n" + r,
                "2. Book a PC", "7. Wallet balance");
        System.out.printf(f + "  ║ " + r + "%-24s" + f + " ║ " + r + "%-25s" + f + " ║%n" + r,
                "3. Booking history", "8. Wallet top up");
        System.out.printf(f + "  ║ " + r + "%-24s" + f + " ║ " + r + "%-25s" + f + " ║%n" + r,
                "4. Cancel booking", "9. Transaction history");
        System.out.printf(f + "  ║ " + r + "%-24s" + f + " ║ " + r + "%-25s" + f + " ║%n" + r,
                "5. Order history", "10. Active session");

        System.out.println(f + "  ╠══════════════════════════════════════════════════════╣" + r);
        System.out.printf(f + "  ║ " + r + "%-52s" + f + " ║%n" + r, "11. Back to main menu");
        System.out.printf(f + "  ║ " + r + "%-52s" + f + " ║%n" + r, "0. Exit");
        System.out.println(f + "  ╚══════════════════════════════════════════════════════╝" + r);
    }

    private void viewBalance() {
        String userId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (userId == null) {
            System.out.println("You need to login to perform this action.");
            pause();
            return;
        }
        System.out.println("\n=== WALLET BALANCE ===");
        System.out.println("Balance: " + walletService.getBalance(userId));
        pause();
    }

    private void topUp() {
        String userId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (userId == null) {
            System.out.println("You need to login to perform this action.");
            pause();
            return;
        }
        System.out.println("\n=== WALLET TOP UP ===");
        System.out.print("Enter amount (VND) (or 0 to return): ");
        if (!ensureHasNextLine()) return;
        String raw = scanner.nextLine().trim();
        if ("0".equals(raw)) return;
        java.math.BigDecimal amount;
        try {
            amount = new java.math.BigDecimal(raw);
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount.");
            pause();
            return;
        }
        walletService.topUp(userId, amount, "Customer top-up");
        System.out.println("[OK] Top-up successful. New balance: " + walletService.getBalance(userId));
        pause();
    }

    private void viewTransactions() {
        String userId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (userId == null) {
            System.out.println("You need to login to perform this action.");
            pause();
            return;
        }
        System.out.println("\n=== RECENT TRANSACTIONS ===");
        java.util.List<Transaction> txs = walletService.getRecentTransactions(userId, 20);
        if (txs.isEmpty()) {
            System.out.println("No transactions.");
            pause();
            return;
        }
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        System.out.println("+------------+---------------+----------+----------------------+------------------------------+");
        System.out.printf("| %-10s | %-13s | %-8s | %-20s | %-28s |%n", "TX ID", "AMOUNT", "TYPE", "TIME", "DESC");
        System.out.println("+------------+---------------+----------+----------------------+------------------------------+");
        for (Transaction t : txs) {
            String time = t.getCreatedAt() == null ? "" : t.getCreatedAt().format(fmt);
            System.out.printf("| %-10s | %-13s | %-8s | %-20s | %-28s |%n",
                    t.getTransactionId(),
                    t.getAmount(),
                    t.getTransactionType() == null ? "" : t.getTransactionType().name(),
                    time,
                    safeShort(t.getDescription()));
        }
        System.out.println("+------------+---------------+----------+----------------------+------------------------------+");
        pause();
    }

    private void viewActiveSession() {
        String userId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (userId == null) {
            System.out.println("You need to login to perform this action.");
            pause();
            return;
        }
        System.out.println("\n=== MY ACTIVE SESSION ===");
        java.util.Optional<com.atmin.saber.model.Booking> active = bookingController.getActiveBooking(userId);
        if (active.isEmpty()) {
            System.out.println("No ACTIVE session.");
        } else {
            var b = active.get();
            System.out.println("PC ID: " + b.getPcId());
            System.out.println("Start: " + (b.getStartTime() == null ? "" : b.getStartTime()));
            System.out.println("Status: " + b.getStatus());

            if (b.getStartTime() != null) {
                long seconds = java.time.temporal.ChronoUnit.SECONDS.between(b.getStartTime(), java.time.LocalDateTime.now());
                long minutes = (long) Math.ceil(Math.max(1, seconds) / 60.0);
                System.out.println("Duration: " + minutes + " minute(s)");

                // Estimate cost so far using same room pricing mapping
                java.math.BigDecimal hourly = estimateHourlyRateByPcId(b.getPcId());
                java.math.BigDecimal perMinute = hourly.divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal cost = perMinute.multiply(java.math.BigDecimal.valueOf(minutes));
                System.out.println("Estimated cost so far: " + cost);
            }
        }
        pause();
    }

    private java.math.BigDecimal estimateHourlyRateByPcId(int pcId) {
        // Keep consistent with existing zone pricing mapping used in BookingService/SessionBilling
        String room = new com.atmin.saber.dao.impl.PcDaoImpl(DBConnection.getInstance())
                .findById(pcId)
                .map(com.atmin.saber.model.PC::getRoomName)
                .orElse("");
        if (room == null) return java.math.BigDecimal.valueOf(10000);
        return switch (room.toLowerCase()) {
            case "atmin2", "atmin5" -> java.math.BigDecimal.valueOf(20000);
            case "atmin3", "atmin6" -> java.math.BigDecimal.valueOf(30000);
            default -> java.math.BigDecimal.valueOf(10000);
        };
    }

    private static String safeShort(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.length() <= 28) return trimmed;
        return trimmed.substring(0, 25) + "...";
    }

    private void viewOrderHistory() {
        String customerId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (customerId == null) {
            System.out.println("You need to login to perform this action.");
            pause();
            return;
        }

        System.out.println("\n=== MY F&B ORDER HISTORY ===");
        java.util.List<Order> orders = orderService.getAllOrdersOfCustomer(customerId);
        if (orders.isEmpty()) {
            System.out.println("You have no orders yet.");
            pause();
            return;
        }

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        System.out.println("+--------------------------------------+----------------------+-----------+---------------+");
        System.out.printf("| %-36s | %-20s | %-9s | %-13s |%n",
                "ORDER ID", "ORDER TIME", "STATUS", "TOTAL");
        System.out.println("+--------------------------------------+----------------------+-----------+---------------+");
        for (Order o : orders) {
            String time = o.getOrderTime() == null ? "" : o.getOrderTime().format(fmt);
            String status = o.getStatus() == null ? "" : o.getStatus().name();
            System.out.printf("| %-36s | %-20s | %-9s | %-13s |%n",
                    o.getOrderId(), time, status, o.getTotalAmount());
        }
        System.out.println("+--------------------------------------+----------------------+-----------+---------------+");

        pause();
    }

    private void viewOrderDetailsById() {
        String customerId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (customerId == null) {
            System.out.println("You need to login to perform this action.");
            pause();
            return;
        }

        System.out.println("\n=== VIEW ORDER DETAILS ===");
        System.out.print("Enter Order ID (or 0 to return): ");
        if (!ensureHasNextLine()) return;
        String orderId = scanner.nextLine().trim();
        if ("0".equals(orderId)) return;

        // Basic ownership check: ensure the order belongs to current user
        boolean owned = orderService.getAllOrdersOfCustomer(customerId).stream()
                .anyMatch(o -> orderId.equals(o.getOrderId()));
        if (!owned) {
            System.out.println("Order not found in your account.");
            pause();
            return;
        }

        java.util.List<OrderDetail> details = orderService.getOrderDetails(orderId);
        if (details.isEmpty()) {
            System.out.println("No items found for this order.");
            pause();
            return;
        }

        System.out.println("+--------------------------------------+-------+----------+---------------+");
        System.out.printf("| %-36s | %-5s | %-8s | %-13s |%n", "DETAIL ID", "PID", "QTY", "UNIT PRICE");
        System.out.println("+--------------------------------------+-------+----------+---------------+");
        for (OrderDetail d : details) {
            System.out.printf("| %-36s | %-5d | %-8d | %-13s |%n",
                    d.getDetailId(),
                    d.getId() == null ? 0 : d.getId(),
                    d.getQuantity() == null ? 0 : d.getQuantity(),
                    d.getUnitPrice());
        }
        System.out.println("+--------------------------------------+-------+----------+---------------+");

        pause();
    }
    
    private void showAvailablePCs() {
        bookingController.showAvailablePCsByZone();
        pause();
    }
    
    private void makeBooking() {
        String customerId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (customerId == null) {
            System.out.println("You need to login to perform this action.");
            pause();
            return;
        }
        
        bookingController.makeBooking(customerId);
        pause();
    }
    
    private void viewBookingHistory() {
        String customerId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (customerId == null) {
            System.out.println("You need to login to perform this action.");
            pause();
            return;
        }
        
        System.out.println("\n=== BOOKING HISTORY ===");
        
        try {
            java.util.List<Booking> bookings = bookingController.getCustomerBookings(customerId);
            if (bookings.isEmpty()) {
                System.out.println("You have no bookings yet.");
            } else {
                System.out.println("+------------+------+----------------------+----------------------+----------------------+---------------+");
                System.out.printf("| %-10s | %-4s | %-20s | %-20s | %-20s | %-13s |%n", 
                    "CUSTOMER", "PC", "START", "END", "STATUS", "FEE");
                System.out.println("+------------+------+----------------------+----------------------+----------------------+---------------+");
                
                for (Booking booking : bookings) {
                    System.out.printf("| %-10s | %-4d | %-20s | %-20s | %-20s | %-13s |%n",
                        booking.getCustomerId().substring(0, 8),
                        booking.getPcId(),
                        booking.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                        booking.getExpectedEndTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                        formatBookingStatus(booking.getStatus()),
                        booking.getTotalFee() + " VND");
                }
                System.out.println("+------------+------+----------------------+----------------------+----------------------+---------------+");
            }
        } catch (Exception e) {
            System.out.println("Error loading booking history: " + e.getMessage());
        }

        pause();
    }
    
    private void cancelBooking() {
        String customerId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (customerId == null) {
            System.out.println("You need to login to perform this action.");
            pause();
            return;
        }
        
        System.out.println("\n=== CANCEL BOOKING ===");
        
        try {
            java.util.List<Booking> bookings = bookingController.getCustomerBookings(customerId);
            java.util.List<Booking> pendingBookings = bookings.stream()
                .filter(b -> b.getStatus().name().equals("PENDING"))
                .toList();
            
            if (pendingBookings.isEmpty()) {
                System.out.println("You have no pending bookings to cancel.");
                pause();
                return;
            }
            
            System.out.println("Your pending bookings:");
            System.out.println("+------------+------+----------------------+----------------------+---------------+");
            System.out.printf("| %-10s | %-4s | %-20s | %-20s | %-13s |%n", 
                "CUSTOMER", "PC", "START", "END", "FEE");
            System.out.println("+------------+------+----------------------+----------------------+---------------+");
            
            for (Booking booking : pendingBookings) {
                System.out.printf("| %-10s | %-4d | %-20s | %-20s | %-13s |%n",
                    booking.getCustomerId().substring(0, 8),
                    booking.getPcId(),
                    booking.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                    booking.getExpectedEndTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                    booking.getTotalFee() + " VND");
            }
            System.out.println("+------------+------+----------------------+----------------------+---------------+");
            
            System.out.print("\nEnter PC ID to cancel (0 to return): ");
            int pcId = readIntAllowBack();
            if (pcId == 0) return;
            
            System.out.print("Enter start time (dd/MM/yyyy HH:mm) (or 0 to return): ");
            if (!ensureHasNextLine()) return;
            String startTimeStr = scanner.nextLine().trim();
            if ("0".equals(startTimeStr)) return;
            java.time.LocalDateTime startTime;
            try {
                startTime = java.time.LocalDateTime.parse(startTimeStr, 
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Invalid time format.");
                pause();
                return;
            }
            
            System.out.print("Are you sure you want to cancel this booking? (Y/N): ");
            if (!ensureHasNextLine()) return;
            String confirm = scanner.nextLine();
            if (confirm.trim().equalsIgnoreCase("Y")) {
                bookingController.cancelBooking(customerId, pcId, startTime);
                System.out.println("[OK] Booking cancelled successfully!");
            } else {
                System.out.println("Operation cancelled.");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pause();
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
        if (!ensureHasNextLine()) return;
        try {
            scanner.nextLine();
        } catch (Exception ignored) {
        }
    }

    private int readIntAllowBack() {
        while (true) {
            if (!ensureHasNextLine()) return 0;
            String raw = scanner.nextLine().trim();
            if (raw.equals("0")) return 0;
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.print("Invalid number. Please enter again (or 0 to return): ");
            }
        }
    }
    
    private String formatBookingStatus(Object status) {
        if (status == null) return "Unknown";
        String statusStr = status.toString();
        return switch (statusStr) {
            case "PENDING" -> "Pending confirmation";
            case "ACTIVE" -> "In use";
            case "COMPLETED" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            default -> statusStr;
        };
    }
}
