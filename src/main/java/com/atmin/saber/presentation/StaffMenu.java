package com.atmin.saber.presentation;

import com.atmin.saber.dao.UserDao;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.enums.OrderStatus;
import com.atmin.saber.service.OrderService;
import com.atmin.saber.util.ConsoleInput;
import com.atmin.saber.config.AppFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import static com.atmin.saber.util.CyberColors.*;

/**
 * Staff console menu:
 * - View pending bookings (PENDING)
 * - View pending F&B orders (PENDING)
 * - Advance order status: PENDING -> COMPLETED
 */
public class StaffMenu extends BaseMenu {
    private final OrderService orderService;
    private final UserDao userDao;

    public StaffMenu(OrderService orderService, UserDao userDao, Scanner scanner) {
        super(scanner);
        this.orderService = orderService;
        this.userDao = userDao;
    }

    public static StaffMenu createDefault(Scanner scanner) {
        return new StaffMenu(AppFactory.orderService(), AppFactory.userDao(), scanner);
    }

    public void showMenu() {
        while (true) {
            System.out.println(CYAN + BOLD + "\n  === STAFF MENU ===" + RESET);
            System.out.println("\t1. View pending F&B orders (FIFO)");
            System.out.println("\t2. Update (advance) F&B order status (one/all)");
            System.out.println("\t3. Reject ONE order (Cancel & Refund)");
            System.out.println("\t0. Back");
            System.out.print(GREEN + "  ➤ Select option: " + RESET);

            String choice = promptMenuChoice(scanner);
            switch (choice) {
                case "1" -> safeRunWithConsoleInputPause(this::viewPendingOrders);
                case "2" -> safeRunWithConsoleInputPause(this::advanceOrderLogic);
                case "3" -> safeRunWithConsoleInputPause(this::rejectOrderLogic);
                case "0" -> {
                    return;
                }
                default -> System.out.println("\tInvalid option. Please try again.");
            }
        }
    }

    private static String promptMenuChoice(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            return "0";
        }
        return scanner.nextLine().trim();
    }

    private void viewPendingOrders() {
        System.out.println("\n=== PENDING F&B ORDERS ===");
        printPendingOrders(true);
    }

    private void advanceOrderLogic() {
        printPendingOrders(false);
        if (orderService.getPendingOrdersForStaff().isEmpty()) {
            ConsoleInput.pressEnterToContinue(scanner);
            return;
        }
        System.out.print("Enter Order ID to advance, or 'ALL' to advance all (0 to return): ");
        String input = readLineOrNull();
        if (input == null || input.trim().isEmpty() || "0".equals(input.trim())) return;

        if ("ALL".equalsIgnoreCase(input.trim())) {
            int updated = orderService.advanceAllPendingOrdersForStaff();
            System.out.println("[OK] Updated " + updated + " pending order(s).");
        } else {
            try {
                int orderId = Integer.parseInt(input.trim());
                OrderStatus next = orderService.advanceOrderStatusForStaff(orderId);
                System.out.println("[OK] Updated status to: " + next.name() + formatStaffMeaning(next));
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid Order ID format. Please enter a number.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        ConsoleInput.pressEnterToContinue(scanner);
    }

    private void rejectOrderLogic() {
        printPendingOrders(false);
        if (orderService.getPendingOrdersForStaff().isEmpty()) {
            ConsoleInput.pressEnterToContinue(scanner);
            return;
        }
        System.out.print("Enter Order ID to REJECT (or 0 to return): ");
        String input = readLineOrNull();
        if (input == null || input.trim().isEmpty() || "0".equals(input.trim())) return;

        try {
            int orderId = Integer.parseInt(input.trim());
            orderService.rejectOrderAndRefund(orderId);
            System.out.println("[OK] Order rejected and refunded.");
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid Order ID format. Please enter a number.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
        ConsoleInput.pressEnterToContinue(scanner);
    }

    private void printPendingOrders(boolean pauseAfter) {
        List<Order> orders = orderService.getPendingOrdersForStaff();
        if (orders.isEmpty()) {
            System.out.println("No pending orders.");
            if (pauseAfter) ConsoleInput.pressEnterToContinue(scanner);
            return;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        System.out.println("+-----+-------+----------+----------+----------------------+-----------+----------+");
        System.out.printf("| %-3s | %-5s | %-8s | %-8s | %-20s | %-9s | %-8s |%n",
                "NO", "ORDID", "CUSTOMER", "CUSTID", "ORDER TIME", "STATUS", "TOTAL");
        System.out.println("+-----+-------+----------+----------+----------------------+-----------+----------+");
        int i = 1;
        for (Order o : orders) {
            String username = getUsernameOrId(o.getCustomerId());
            System.out.printf("| %-3d | %-5d | %-8s | %-8s | %-20s | %-9s | %-8s |%n",
                    i++, o.getOrderId(),
                    safeLength(username),
                    safeLength(o.getCustomerId()),
                    o.getOrderTime() == null ? "" : o.getOrderTime().format(fmt),
                    o.getStatus() == null ? "" : o.getStatus().name(),
                    o.getTotalAmount() == null ? "0.00" : safeLength(o.getTotalAmount().toString()));
        }
        System.out.println("+-----+-------+----------+----------+----------------------+-----------+----------+");
        if (pauseAfter) ConsoleInput.pressEnterToContinue(scanner);
    }

    private static String formatStaffMeaning(OrderStatus status) {
        return switch (status) {
            case PENDING -> " (Confirmed)";
            case COMPLETED -> " (Completed)";
            case PAID -> " (Paid)";
            case CANCELLED -> " (Cancelled)";
        };
    }


    private String getUsernameOrId(String customerId) {
        try {
            return userDao.findById(customerId)
                    .map(user -> user.getUsername() != null ? user.getUsername() : customerId)
                    .orElse(customerId);
        } catch (Exception e) {
            return customerId;
        }
    }
}
