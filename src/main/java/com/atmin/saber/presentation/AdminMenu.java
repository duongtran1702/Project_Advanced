package com.atmin.saber.presentation;
import com.atmin.saber.util.SessionContext;

import com.atmin.saber.dao.impl.StatisticsDaoImpl;
import com.atmin.saber.service.StatisticsService;
import com.atmin.saber.service.impl.StatisticsServiceImpl;
import com.atmin.saber.util.DBConnection;

import java.time.YearMonth;
import java.util.List;

import java.util.Scanner;

import static com.atmin.saber.util.CyberColors.*;

public class AdminMenu {

    private AdminMenu() {
    }

    public static void start(Scanner sc) {
        StatisticsService statisticsService = new StatisticsServiceImpl(new StatisticsDaoImpl(DBConnection.getInstance()));
        while (true) {
            System.out.println(CYAN + BOLD + "  === ADMIN SYSTEM ===" + RESET);
            System.out.println("\t1. PC Management ");
            System.out.println("\t2. Food and Beverage Service Management");
            System.out.println("\t3. Monthly statistics report");
            System.out.println("\t0. Logout");
            System.out.print(GREEN + "  ➤ Your choice: " + RESET);

            if (!sc.hasNextLine()) {
                SessionContext.clearCurrentRole();
                SessionContext.clearCurrentUser();
                System.out.println("\n[EOF] No more input. Logout...");
                return;
            }
            String input = sc.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("\tPlease enter a number.");
                continue;
            }

            switch (choice) {
                case 1 -> PcManagementMenu.start(sc);
                case 2 -> FnBManagementMenu.start(sc);
                case 3 -> showMonthlyStatistics(sc, statisticsService);
                case 0 -> {
                    SessionContext.clearCurrentRole();
                    SessionContext.clearCurrentUser();
                    return;
                }
                default -> System.out.println("\tInvalid choice.");
            }
        }
    }

    private static void showMonthlyStatistics(Scanner sc, StatisticsService statisticsService) {
        System.out.print("Enter year (e.g. 2026): ");
        if (!sc.hasNextLine()) {
            System.out.println("\n[EOF] No more input. Returning...");
            return;
        }
        String raw = sc.nextLine().trim();
        int year;
        try {
            year = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            System.out.println("Invalid year.");
            return;
        }

        List<StatisticsService.MonthlyRow> rows = statisticsService.monthlyReport(year);
        System.out.println("\n=== MONTHLY STATISTICS: " + year + " ===");
        System.out.println("+---------+----------------+----------------+----------------+----------------+");
        System.out.printf("| %-7s | %-14s | %-14s | %-14s | %-14s |%n",
                "Month", "Session Rev", "F&B Rev", "Topup", "Payments");
        System.out.println("+---------+----------------+----------------+----------------+----------------+");
        for (StatisticsService.MonthlyRow r : rows) {
            YearMonth ym = r.month();
            System.out.printf("| %4d-%02d | %-14s | %-14s | %-14s | %-14s |%n",
                    ym.getYear(), ym.getMonthValue(),
                    r.sessionRevenue(), r.fnbRevenue(), r.topup(), r.payment());
        }
        System.out.println("+---------+----------------+----------------+----------------+----------------+");
        System.out.print("Press Enter to return...");
        if (sc.hasNextLine()) {
            sc.nextLine();
        }
    }
}
