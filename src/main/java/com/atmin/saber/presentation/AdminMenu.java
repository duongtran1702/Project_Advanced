package com.atmin.saber.presentation;

import com.atmin.saber.util.SessionContext;
import com.atmin.saber.dao.impl.StatisticsDaoImpl;
import com.atmin.saber.service.StatisticsService;
import com.atmin.saber.service.impl.StatisticsServiceImpl;
import com.atmin.saber.util.DBConnection;
import com.atmin.saber.util.ConsoleInput;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

import static com.atmin.saber.util.CyberColors.*;

public class AdminMenu extends BaseMenu {
    private final StatisticsService statisticsService;

    private AdminMenu(Scanner scanner) {
        super(scanner);
        this.statisticsService = new StatisticsServiceImpl(
                new StatisticsDaoImpl(DBConnection.getInstance()));
    }

    public static void start(Scanner sc) {
        new AdminMenu(sc).showMenu();
    }

    private void showMenu() {
        while (true) {
            System.out.println(CYAN + BOLD + "  === ADMIN SYSTEM ===" + RESET);
            System.out.println("\t1. PC Management ");
            System.out.println("\t2. Food and Beverage Service Management");
            System.out.println("\t3. Daily statistics report");
            System.out.println("\t0. Logout");
            int choice;
            try {
                choice = ConsoleInput.readInt(scanner, GREEN + "  ➤ Your choice: " + RESET,
                        "\tPlease enter a valid number.");
            } catch (ConsoleInput.EndOfInputException e) {
                SessionContext.clearCurrentRole();
                SessionContext.clearCurrentUser();
                System.out.println("\n[EOF] No more input. Logout...");
                return;
            }

            switch (choice) {
                case 1 -> PcManagementMenu.start(scanner);
                case 2 -> FnBManagementMenu.start(scanner);
                case 3 -> safeRun(this::showDailyStatistics);
                case 0 -> {
                    SessionContext.clearCurrentRole();
                    SessionContext.clearCurrentUser();
                    return;
                }
                default -> System.out.println("\tInvalid choice.");
            }
        }
    }

    private void showDailyStatistics() {
        System.out.print("Enter month for daily report (e.g. 2026-04): ");
        String raw = readLineOrNull();
        if (raw == null) return;

        YearMonth targetMonth;
        try {
            targetMonth = YearMonth.parse(raw.trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid format. Please use YYYY-MM (e.g., 2026-04).");
            return;
        }

        List<StatisticsService.DailyRow> rows = statisticsService.dailyReport(targetMonth);
        System.out.println("\n=== DAILY STATISTICS: " + targetMonth + " ===");
        System.out.println("+------------+----------------+----------------+----------------+----------------+----------------+");
        System.out.printf("| %-10s | %-14s | %-14s | %-14s | %-14s | %-14s |%n",
                "Date", "Session Rev", "F&B Rev", "Topup", "Payments", "Refunds");
        System.out.println("+------------+----------------+----------------+----------------+----------------+----------------+");

        for (StatisticsService.DailyRow r : rows) {
            System.out.printf("| %-10s | %-14s | %-14s | %-14s | %-14s | %-14s |%n",
                    r.date(),
                    formatAmount(r.sessionRevenue()),
                    formatAmount(r.fnbRevenue()),
                    formatAmount(r.topup()),
                    formatAmount(r.payment()),
                    formatAmount(r.refund()));
        }
        System.out.println("+------------+----------------+----------------+----------------+----------------+----------------+");
    }

    private static String formatAmount(java.math.BigDecimal amount) {
        return amount == null ? "0.00" : amount.abs().toPlainString();
    }
}
