package com.atmin.saber.presentation;

import com.atmin.saber.controller.BookingController;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.User;
import com.atmin.saber.model.Product;
import com.atmin.saber.service.OrderService;
import com.atmin.saber.service.ProductService;
import com.atmin.saber.service.WalletService;
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
                case 2 -> safeRun(this::toggleSession);
                case 3 -> safeRun(this::orderFoodAndDrink);
                case 4 -> safeRun(this::viewOrderHistory);
                case 5 -> safeRun(this::viewBalance);
                case 6 -> safeRun(this::topUp);
                case 7 -> {
                    return;
                }

                default -> System.out.println("Invalid choice. Please select from 1-7.");
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

        String userId = SessionContext.getCurrentUser().map(User::getUserId).orElse(null);
        boolean hasSession = (userId != null) && bookingController.getActiveBooking(userId).isPresent();
        // Đánh giá trạng thái máy để hiển thị text phù hợp
        String sessionAction = hasSession ? "2. Finish session" : "2. Start session";

        // Menu gọn gàng có tích hợp Nạp tiền mô phỏng (1-7)
        printDashboardRow(f, r, "1. View available PCs", "5. Wallet balance");
        printDashboardRow(f, r, sessionAction,         "6. Top up (Simulation)");
        printDashboardRow(f, r, "3. Order (F&B)",      "7. Back");
        printDashboardRow(f, r, "4. Order history",    "");
        System.out.println(f + "  ╚══════════════════════════════════════════════════════╝" + r);
        while (true) {
            System.out.print(GREEN + "  ➤ Your choice: " + RESET);
            String raw = readLineOrNull();
            if (raw == null) return 7;
            String choice = raw.trim();
            try {
                return Integer.parseInt(choice);
            } catch (NumberFormatException ignored) {
                System.out.println("Invalid choice. Please select from 1-7.");
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

    // viewActiveSession() removed from menu: auto-stop worker + finishSession handle session lifecycle.

    /**
     * Spec requirement: customer can start playing immediately if PC is AVAILABLE and wallet can pay at least 1 minute.
     * We create an ACTIVE booking record and set PC status to IN_USE.
     * If user already has an active session, this toggles to Finish session behavior.
     */
    private void toggleSession() {
        String customerId = requireLoginOrBack();
        if (customerId == null) return;

        if (bookingController.getActiveBooking(customerId).isPresent()) {
            System.out.println("\n=== FINISH SESSION ===");
            java.util.Optional<java.math.BigDecimal> charged = bookingController.stopActiveBookingAndCharge(customerId);
            if (charged.isEmpty()) {
                System.out.println("No ACTIVE session.");
                return;
            }
            System.out.println("[OK] Session finished. Charged: " + charged.get());
            System.out.println("New balance: " + walletService.getBalance(customerId));
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
            
            // Tính toán trên RAM
            java.math.BigDecimal currentDebt = bookingController.calculateCurrentDebt(customerId);
            java.math.BigDecimal balance = walletService.getBalance(customerId);
            if (balance == null) balance = java.math.BigDecimal.ZERO;
            java.math.BigDecimal availableBalance = balance.subtract(currentDebt);

            if (availableBalance.compareTo(line) < 0) {
                System.out.println("Insufficient funds! Available (minus PC debt): " + availableBalance + " VND. Item costs: " + line + " VND.");
                continue;
            }

            // Khoá ví và trừ ngay tiền món ăn vào ví trong Database (WalletService.charge uses row lock natively)
            boolean ok = walletService.charge(customerId, line, "F&B Ordered: " + p.getProductName() + " x" + qty);
            if (!ok) {
                System.out.println("Failed to charge wallet. Please try again.");
                continue;
            }

            // Tạo đơn hàng ngay lập tức
            java.util.Map<Integer, Integer> itemsMap = java.util.Collections.singletonMap(pid, qty);
            OrderService.CreatedOrder created = orderService.createOrderForBooking(customerId, active, itemsMap);
            
            System.out.println("[OK] Order placed successfully! Order ID: " + created.order().getOrderId());
            System.out.println("Deducted " + line + " VND. Remaining Available Balance: " + availableBalance.subtract(line) + " VND.");

            System.out.print("\nDo you want to order more? (Y/N): ");
            String moreLine = readLineOrNull();
            if (moreLine == null) break;
            String more = moreLine.trim();
            if (!more.equalsIgnoreCase("Y")) {
                break;
            }
        }
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

        // Allow customer to input an order id to view details (items)
        while (true) {
            System.out.print("Enter Order ID to view details (or 0 to return): ");
            String rawLine = readLineOrNull();
            if (rawLine == null) return;
            String input = rawLine.trim();
            if (input.isEmpty() || "0".equals(input)) return;

            try {
                int orderId = Integer.parseInt(input);
                boolean belongsToCustomer = orders.stream().anyMatch(o -> orderId == o.getOrderId());
                if (!belongsToCustomer) {
                    System.out.println("Order ID not found in your history.");
                    continue;
                }

                java.util.List<com.atmin.saber.model.OrderDetail> details;
                try {
                    details = orderService.getOrderDetails(orderId);
                } catch (RuntimeException ex) {
                    System.out.println("Cannot load order details: " + ex.getMessage());
                    continue;
                }

                if (details == null || details.isEmpty()) {
                    System.out.println("No items for this order.");
                    continue;
                }

                System.out.println("\n=== ORDER DETAILS: " + orderId + " ===");
                System.out.println("+------+----------------------+----------+---------------+");
                System.out.printf("| %-4s | %-20s | %-8s | %-13s |%n", "ID", "NAME", "QTY", "UNIT PRICE");
                System.out.println("+------+----------------------+----------+---------------+");
                for (com.atmin.saber.model.OrderDetail d : details) {
                    int pid = d.getId();
                    String name = productService.getById(pid).map(Product::getProductName).orElse("(missing)");
                    System.out.printf("| %-4d | %-20s | %-8d | %-13s |%n",
                            pid,
                            safeShort(name),
                            d.getQuantity() == null ? 0 : d.getQuantity(),
                            d.getUnitPrice() == null ? "0" : d.getUnitPrice().toString());
                }
                System.out.println("+------+----------------------+----------+---------------+");
            } catch (NumberFormatException e) {
                System.out.println("Invalid Order ID format. Please enter a number.");
            }
        }
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
