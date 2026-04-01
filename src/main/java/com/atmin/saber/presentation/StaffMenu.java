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
 * - View pending F&B orders (PENDING / PREPARING)
 * - Advance order status: PENDING -> PREPARING -> SERVED
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
            System.out.println("\t0. Back");
            System.out.print(GREEN + "  ➤ Select option: " + RESET);

            String choice = promptMenuChoice(scanner);
            switch (choice) {
                case "1" -> safeRun(this::viewPendingOrders);
                case "2" -> safeRun(this::advanceOrderStatus);
                case "0" -> {
                    return;
                }
                default -> System.out.println("\tInvalid option. Please try again.");
            }
        }
    }

    private static String promptMenuChoice(Scanner scanner) {
        while (true) {
            if (!scanner.hasNextLine()) {
                System.out.println("\n\tNo more input. Returning...");
                return "0";
            }
            String input = scanner.nextLine().trim();
            switch (input) {
                case "0", "1", "2" -> {
                    return input;
                }
                default -> {
                    System.out.print("\tInvalid choice.\n");
                    System.out.print(GREEN + "  ➤ Select option: " + RESET);
                }
            }
        }
    }

    private void viewPendingOrders() {
        System.out.println("\n=== PENDING F&B ORDERS ===");
        printPendingOrders(true);
    }

    private void advanceOrderStatus() {
        while (true) {
            System.out.println("\n=== UPDATE (ADVANCE) F&B ORDER STATUS ===");
            System.out.println("  1. Update ONE order (advance 1 step)");
            System.out.println("  2. Update ALL pending orders (FIFO, advance 1 step each)");
            System.out.println("  0. Back");
            System.out.print("-> Select option (0-2): ");

            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> {
                    // show list first so staff can copy/paste orderId (no pause)
                    printPendingOrders(false);
                    System.out.print("Enter orderId (or 0 to return): ");
                    if (!scanner.hasNextLine()) return;
                    String orderId = scanner.nextLine().trim();
                    if ("0".equals(orderId)) continue;

                    OrderStatus next = orderService.advanceOrderStatusForStaff(orderId);
                    System.out.println("[OK] Updated status to: " + next.name() + formatStaffMeaning(next));
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case "2" -> {
                    int updated = orderService.advanceAllPendingOrdersForStaff();
                    System.out.println("[OK] Updated " + updated + " pending order(s).");
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case "0" -> {
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
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
            ConsoleInput.pressEnterToContinue(scanner);
        }
    }
}
