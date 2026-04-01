package com.atmin.saber.presentation;

import com.atmin.saber.util.SessionContext;

import com.atmin.saber.dao.impl.StatisticsDaoImpl;
import com.atmin.saber.service.StatisticsService;
import com.atmin.saber.service.impl.StatisticsServiceImpl;
import com.atmin.saber.util.DBConnection;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import java.util.Scanner;

import static com.atmin.saber.util.CyberColors.*;

public class AdminMenu {

    private AdminMenu() {
    }

    public static void start(Scanner sc) {
        StatisticsService statisticsService = new StatisticsServiceImpl(
                new StatisticsDaoImpl(DBConnection.getInstance()));
        while (true) {
            System.out.println(CYAN + BOLD + "  === ADMIN SYSTEM ===" + RESET);
            System.out.println("\t1. PC Management ");
            System.out.println("\t2. Food and Beverage Service Management");
            System.out.println("\t3. Daily statistics report");
            System.out.println("\t0. Logout");
            int choice;
            try {
                choice = com.atmin.saber.util.ConsoleInput.readInt(sc, GREEN + "  ➤ Your choice: " + RESET,
                        "\tPlease enter a valid number.");
            } catch (com.atmin.saber.util.ConsoleInput.EndOfInputException e) {
                SessionContext.clearCurrentRole();
                SessionContext.clearCurrentUser();
                System.out.println("\n[EOF] No more input. Logout...");
                return;
            }

            switch (choice) {
                case 1 -> PcManagementMenu.start(sc);
                case 2 -> FnBManagementMenu.start(sc);
                case 3 -> showDailyStatistics(sc, statisticsService);
                case 0 -> {
                    SessionContext.clearCurrentRole();
                    SessionContext.clearCurrentUser();
                    return;
                }
                default -> System.out.println("\tInvalid choice.");
            }
        }
    }

    private static void showDailyStatistics(Scanner sc, StatisticsService statisticsService) {
        System.out.print("Enter month for daily report (e.g. 2026-04): ");
        if (!sc.hasNextLine()) {
            System.out.println("\n[EOF] No more input. Returning...");
            return;
        }
        String raw = sc.nextLine().trim();
        YearMonth targetMonth;

        try {
            // Parse chuỗi nhập vào theo chuẩn yyyy-MM
            targetMonth = YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid format. Please use YYYY-MM (e.g., 2026-04).");
            return;
        }

        List<StatisticsService.DailyRow> rows = statisticsService.dailyReport(targetMonth);

        System.out.println("\n=== DAILY STATISTICS: " + targetMonth + " ===");
        System.out.println("+------------+----------------+----------------+----------------+----------------+");
        System.out.printf("| %-10s | %-14s | %-14s | %-14s | %-14s |%n",
                "Date", "Session Rev", "F&B Rev", "Topup", "Payments");
        System.out.println("+------------+----------------+----------------+----------------+----------------+");

        for (StatisticsService.DailyRow r : rows) {
            LocalDate date = r.date();
            System.out.printf("| %-10s | %-14s | %-14s | %-14s | %-14s |%n",
                    date,
                    r.sessionRevenue(),
                    r.fnbRevenue(),
                    r.topup(),
                    r.payment());
        }

        System.out.println("+------------+----------------+----------------+----------------+----------------+");
        System.out.print("Press Enter to return...");
        if (sc.hasNextLine()) sc.nextLine();
    }
}
