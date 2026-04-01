package com.atmin.saber.presentation;

import com.atmin.saber.controller.BookingController;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.User;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.model.Product;
import com.atmin.saber.service.OrderService;
import com.atmin.saber.service.ProductService;
import com.atmin.saber.service.WalletService;
import com.atmin.saber.model.Transaction;
import com.atmin.saber.config.AppFactory;
import com.atmin.saber.util.CyberColors;
import com.atmin.saber.util.SessionContext;

import java.util.Scanner;

import static com.atmin.saber.util.CyberColors.GREEN;
import static com.atmin.saber.util.CyberColors.RESET;

public class CustomerMenu {
    private final BookingController bookingController;
    private final OrderService orderService;
    private final WalletService walletService;
    private final ProductService productService;
    private final Scanner scanner;

    public CustomerMenu(BookingController bookingController,
                        OrderService orderService,
                        WalletService walletService,
                        ProductService productService,
                        Scanner scanner) {
        this.bookingController = bookingController;
        this.orderService = orderService;
        this.walletService = walletService;
        this.productService = productService;
        this.scanner = scanner;
    }

    public static CustomerMenu createDefault(Scanner scanner) {
        return new CustomerMenu(
                BookingController.createDefault(),
                AppFactory.orderService(),
                AppFactory.walletService(),
                AppFactory.productService(),
                scanner
        );
    }

    private String readLineOrNull() {
        if (scanner == null || !scanner.hasNextLine()) return null;
        return scanner.nextLine();
    }

    private String requireLoginOrBack() {
        String userId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        if (userId == null) {
            System.out.println("You need to login to perform this action.");
            return null;
        }
        return userId;
    }

    public void showMenu() {
        while (true) {
            int choice = showDashboard();
            switch (choice) {
                case 1 -> safeRun(this::showAvailablePCs);
                case 2 -> safeRun(this::startSession);
                case 3 -> safeRun(this::finishSession);
                case 4 -> safeRun(this::orderFoodAndDrink);
                case 5 -> safeRun(this::viewOrderHistory);
                case 6 -> safeRun(this::viewOrderDetailsById);
                case 7 -> safeRun(this::viewBalance);
                case 8 -> safeRun(this::topUp);
                case 9 -> safeRun(this::viewTransactions);
                case 10 -> {
                    return;
                }

                default -> System.out.println("Invalid choice. Please select from 1-10.");
            }
        }
    }

    private int showDashboard() {
        String f = CyberColors.CYAN;
        String r = CyberColors.RESET;
        String b = CyberColors.BOLD;
        System.out.println(f + "  ╔══════════════════════════════════════════════════════╗" + r);
        System.out.printf(f + "  ║ " + r + b + "%-52s" + f + " ║%n" + r, "                CUSTOMER DASHBOARD");
        System.out.println(f + "  ╠══════════════════════════╦═══════════════════════════╣" + r);

        // Menu (1-10)
        printDashboardRow(f, r, "1. View available PCs", "6. Order details");
        printDashboardRow(f, r, "2. Start session", "7. Wallet balance");
        printDashboardRow(f, r, "3. Finish session", "8. Wallet top up");
        printDashboardRow(f, r, "4. Order (F&B)", "9. Transaction history");
        printDashboardRow(f, r, "5. Order history", "10. Back");
        System.out.println(f + "  ╚══════════════════════════════════════════════════════╝" + r);
        while (true) {
            System.out.print(GREEN + "  ➤ Your choice: " + RESET);
            String raw = readLineOrNull();
            if (raw == null) return 10;
            String choice = raw.trim();
            try {
                return Integer.parseInt(choice);
            } catch (NumberFormatException ignored) {
                System.out.println("Invalid choice. Please select from 0-10.");
            }
        }
    }

    private static void printDashboardRow(String f, String r, String left, String right) {
        System.out.printf(f + "  ║ " + r + "%-24s" + f + " ║ " + r + "%-25s" + f + " ║%n" + r, left, right);
    }

    private void viewBalance() {
        String userId = requireLoginOrBack();
        if (userId == null) return;
        System.out.println("\n=== WALLET BALANCE ===");
        System.out.println("Balance: " + walletService.getBalance(userId));
    }

    private void topUp() {
        String userId = requireLoginOrBack();
        if (userId == null) return;
        System.out.println("\n=== WALLET TOP UP ===");
        System.out.print("Enter amount (VND) (or 0 to return): ");
        String rawLine = readLineOrNull();
        if (rawLine == null) return;
        String raw = rawLine.trim();
        if ("0".equals(raw)) return;
        java.math.BigDecimal amount;
        try {
            amount = new java.math.BigDecimal(raw);
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount.");
            return;
        }
        walletService.topUp(userId, amount, "Customer top-up");
        System.out.println("[OK] Top-up successful. New balance: " + walletService.getBalance(userId));
    }

    private void viewTransactions() {
        String userId = requireLoginOrBack();
        if (userId == null) return;
        System.out.println("\n=== RECENT TRANSACTIONS ===");
        java.util.List<Transaction> txs = walletService.getRecentTransactions(userId, 20);
        if (txs.isEmpty()) {
            System.out.println("No transactions.");
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
    }

    // viewActiveSession() removed from menu: auto-stop worker + finishSession handle session lifecycle.

    /**
     * Spec requirement: customer can start playing immediately if PC is AVAILABLE and wallet can pay at least 1 minute.
     * We create an ACTIVE booking record and set PC status to IN_USE.
     */
    private void startSession() {
        String customerId = requireLoginOrBack();
        if (customerId == null) return;

        // Prevent multiple active sessions
        if (bookingController.getActiveBooking(customerId).isPresent()) {
            System.out.println("You already have an ACTIVE session. Please finish it first.");
            return;
        }

        System.out.println("\n=== START SESSION ===");
        bookingController.showAvailablePCsByZone();
        System.out.print("Enter PC ID to start (0 to return): ");
        int pcId = readIntAllowBack();
        if (pcId == 0) return;

        // Wallet check: must pay at least 1 minute
        java.math.BigDecimal hourly = estimateHourlyRateByPcId(pcId);
        java.math.BigDecimal perMinute = hourly.divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal balance = walletService.getBalance(customerId);
        if (balance == null) balance = java.math.BigDecimal.ZERO;
        if (perMinute.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            System.out.println("Invalid pricing configuration.");
            return;
        }
        if (balance.compareTo(perMinute) < 0) {
            System.out.println("Insufficient balance to start. Minimum required for 1 minute: " + perMinute);
            return;
        }

        // Customer starts an ACTIVE session immediately
        bookingController.startBooking(customerId, pcId, java.time.LocalDateTime.now());

        long payableMinutes = balance.divide(perMinute, 0, java.math.RoundingMode.FLOOR).longValue();
        System.out.println("[OK] Session started on PC " + pcId);
        System.out.println("You can play about " + payableMinutes + " minute(s) with current balance.");
    }

    private void finishSession() {
        String customerId = requireLoginOrBack();
        if (customerId == null) return;

        System.out.println("\n=== FINISH SESSION ===");
        java.util.Optional<java.math.BigDecimal> charged = bookingController.stopActiveBookingAndCharge(customerId);
        if (charged.isEmpty()) {
            System.out.println("No ACTIVE session.");
            return;
        }
        System.out.println("[OK] Session finished. Charged: " + charged.get());
        System.out.println("New balance: " + walletService.getBalance(customerId));
    }

    /**
     * F&B Order flow:
     * - show products
     * - user enters items
     * - charge wallet per item (reserve)
     * - confirm; if cancel => refund reserved
     * - if you confirm => create order linked to current ACTIVE booking
     */
    private void orderFoodAndDrink() {
        String customerId = requireLoginOrBack();
        if (customerId == null) return;

        System.out.println("\n=== ORDER FOOD & DRINK ===");

        java.util.Optional<Booking> activeOpt = bookingController.getActiveBooking(customerId);
        if (activeOpt.isEmpty()) {
            System.out.println("You must start a session before ordering F&B.");
            return;
        }
        Booking active = activeOpt.get();

        java.util.List<Product> products = productService.getAll();
        if (products.isEmpty()) {
            System.out.println("No products available.");
            return;
        }

        java.util.Map<Integer, Integer> items = new java.util.LinkedHashMap<>();
        java.util.List<java.math.BigDecimal> chargedAmounts = new java.util.ArrayList<>();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;

        while (true) {
            printProductMenu(products);
            System.out.print("Enter product id (0 to stop): ");
            int pid = readIntAllowBack();
            if (pid == 0) break;

            Product p = productService.getById(pid).orElse(null);
            if (p == null) {
                System.out.println("Invalid product id.");
                continue;
            }

            System.out.print("Enter quantity: ");
            int qty = readIntAllowBack();
            if (qty <= 0) {
                System.out.println("Quantity must be > 0.");
                continue;
            }

            java.math.BigDecimal line = p.getPrice().multiply(java.math.BigDecimal.valueOf(qty));
            boolean ok = walletService.charge(customerId, line, "F&B item reserved: " + p.getProductName());
            if (!ok) {
                System.out.println("Insufficient balance for this item.");
                continue;
            }

            items.merge(pid, qty, Integer::sum);
            chargedAmounts.add(line);
            total = total.add(line);

            System.out.println("[OK] Added: " + p.getProductName() + " x" + qty + " (" + line + ")");
            System.out.println("Current total reserved: " + total);

            System.out.print("Add more? (Y/N): ");
            String moreLine = readLineOrNull();
            if (moreLine == null) break;
            String more = moreLine.trim();
            if (!more.equalsIgnoreCase("Y")) break;
        }

        if (items.isEmpty()) {
            System.out.println("No items selected.");
            return;
        }

        System.out.println("\n=== ORDER SUMMARY ===");
        System.out.println("+------+----------------------+----------+---------------+---------------+");
        System.out.printf("| %-4s | %-20s | %-8s | %-13s | %-13s |%n", "ID", "NAME", "QTY", "UNIT", "LINE");
        System.out.println("+------+----------------------+----------+---------------+---------------+");
        for (var e : items.entrySet()) {
            Product p = productService.getById(e.getKey()).orElse(null);
            if (p == null) continue;
            int qty = e.getValue();
            java.math.BigDecimal line = p.getPrice().multiply(java.math.BigDecimal.valueOf(qty));
            System.out.printf("| %-4d | %-20s | %-8d | %-13s | %-13s |%n",
                    p.getId(), safeShort(p.getProductName()), qty, p.getPrice(), line);
        }
        System.out.println("+------+----------------------+----------+---------------+---------------+");
        System.out.println("TOTAL: " + total);

        System.out.print("Confirm order? (Y/N): ");
        String confirmLine = readLineOrNull();
        if (confirmLine == null) return;
        String confirm = confirmLine.trim();
        if (!confirm.equalsIgnoreCase("Y")) {
            for (java.math.BigDecimal amt : chargedAmounts) {
                walletService.topUp(customerId, amt, "Refund reserved F&B (cancelled)");
            }
            System.out.println("Order cancelled. Reserved amount refunded.");
            return;
        }

        OrderService.CreatedOrder created = orderService.createOrderForBooking(customerId, active, items);
        System.out.println("[OK] Order created. Order ID: " + created.order().getOrderId());
        System.out.println("Status: " + created.order().getStatus());
    }

    private void printProductMenu(java.util.List<Product> products) {
        System.out.println("\n=== F&B MENU ===");
        System.out.println("+------+----------------------+---------------+----------+");
        System.out.printf("| %-4s | %-20s | %-13s | %-8s |%n", "ID", "NAME", "PRICE", "STOCK");
        System.out.println("+------+----------------------+---------------+----------+");
        for (Product p : products) {
            System.out.printf("| %-4d | %-20s | %-13s | %-8d |%n",
                    p.getId(), safeShort(p.getProductName()), p.getPrice(), p.getStockQuantity());
        }
        System.out.println("+------+----------------------+---------------+----------+");
    }

    private java.math.BigDecimal estimateHourlyRateByPcId(int pcId) {
        // Prefer DB room.base_price if available; fallback to mapping.
        try {
            String sql = "SELECT r.base_price FROM pcs p JOIN rooms r ON p.zone_id = r.room_id WHERE p.pc_id = ? LIMIT 1";
            try (java.sql.Connection con = AppFactory.db().getConnection();
                 java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, pcId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        java.math.BigDecimal price = rs.getBigDecimal(1);
                        if (price != null) return price;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        String room = "";
        try {
            room = new com.atmin.saber.dao.impl.PcDaoImpl(AppFactory.db())
                    .findById(pcId)
                    .map(com.atmin.saber.model.PC::getRoomName)
                    .orElse("");
        } catch (RuntimeException ignored) {
        }
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
        String customerId = requireLoginOrBack();
        if (customerId == null) return;

        System.out.println("\n=== MY F&B ORDER HISTORY ===");
        java.util.List<Order> orders = orderService.getAllOrdersOfCustomer(customerId);
        if (orders.isEmpty()) {
            System.out.println("You have no orders yet.");
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
    }

    private void viewOrderDetailsById() {
        String customerId = requireLoginOrBack();
        if (customerId == null) return;

        System.out.println("\n=== VIEW ORDER DETAILS ===");
        System.out.print("Enter Order ID (or 0 to return): ");
        String orderIdLine = readLineOrNull();
        if (orderIdLine == null) return;
        String orderId = orderIdLine.trim();
        if ("0".equals(orderId)) return;

        // Ownership check without loading all orders (more scalable)
        boolean owned = false;
        try {
            owned = orderService.getAllOrdersOfCustomer(customerId).stream().anyMatch(o -> orderId.equals(o.getOrderId()));
        } catch (RuntimeException ignored) {
        }
        if (!owned) {
            System.out.println("Order not found in your account.");
            return;
        }

        java.util.List<OrderDetail> details = orderService.getOrderDetails(orderId);
        if (details.isEmpty()) {
            System.out.println("No items found for this order.");
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
    }

    private void showAvailablePCs() {
        bookingController.showAvailablePCsByZone();
    }

    // cancelBooking() removed: customer flow starts sessions directly; no PENDING bookings to cancel.

    private void safeRun(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        pause();
    }

    private void pause() {
        System.out.print("\nPress Enter to return to menu...");
        readLineOrNull();
    }

    private int readIntAllowBack() {
        while (true) {
            String rawLine = readLineOrNull();
            if (rawLine == null) return 0;
            String raw = rawLine.trim();
            if (raw.equals("0")) return 0;
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.print("Invalid number. Please enter again (or 0 to return): ");
            }
        }
    }

}
