package com.atmin.saber.presentation;

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
public class StaffMenu {
    private final OrderService orderService;
    private final Scanner scanner;

    public StaffMenu(OrderService orderService, Scanner scanner) {
        this.orderService = orderService;
        this.scanner = scanner;
    }

    public static StaffMenu createDefault(Scanner scanner) {
        return new StaffMenu(AppFactory.orderService(), scanner);
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
                case "1" -> safeRun(this::viewPendingOrders);
                case "2" -> safeRun(this::advanceOrderLogic);
                case "3" -> safeRun(this::rejectOrderLogic);
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
        if (!scanner.hasNextLine()) return;
        
        String input = scanner.nextLine().trim();
        if ("0".equals(input) || input.isEmpty()) {
            return;
        }

        if ("ALL".equalsIgnoreCase(input)) {
            int updated = orderService.advanceAllPendingOrdersForStaff();
            System.out.println("[OK] Updated " + updated + " pending order(s).");
        } else {
            try {
                int orderId = Integer.parseInt(input);
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
        if (!scanner.hasNextLine()) return;
        
        String input = scanner.nextLine().trim();
        if ("0".equals(input) || input.isEmpty()) {
            return;
        }

        try {
            int orderId = Integer.parseInt(input);
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
        System.out.println("+------+--------------------------------------+------------+----------------------+-----------+---------------+");
        System.out.printf("| %-4s | %-36s | %-10s | %-20s | %-9s | %-13s |%n",
                "NO", "ORDER ID", "CUSTOMER", "ORDER TIME", "STATUS", "TOTAL");
        System.out.println("+------+--------------------------------------+------------+----------------------+-----------+---------------+");
        int i = 1;
        for (Order o : orders) {
            System.out.printf("| %-4d | %-36s | %-10s | %-20s | %-9s | %-13s |%n",
                    i++,
                    o.getOrderId(),
                    shortId(o.getCustomerId()),
                    o.getOrderTime() == null ? "" : o.getOrderTime().format(fmt),
                    o.getStatus() == null ? "" : o.getStatus().name(),
                    o.getTotalAmount());
        }
        System.out.println("+------+--------------------------------------+------------+----------------------+-----------+---------------+");
        if (pauseAfter) ConsoleInput.pressEnterToContinue(scanner);
    }

    private static String formatStaffMeaning(OrderStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> " (Confirmed)";
            case COMPLETED -> " (Completed)";
            case PAID -> " (Paid)";
            case CANCELLED -> " (Cancelled)";
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
            ConsoleInput.pressEnterToContinue(scanner);
        }
    }
}
