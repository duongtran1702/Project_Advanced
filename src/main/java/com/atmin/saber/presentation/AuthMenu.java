package com.atmin.saber.presentation;

import com.atmin.saber.model.User;
import com.atmin.saber.model.enums.UserRole;
import com.atmin.saber.util.ConsoleInput;
import com.atmin.saber.util.SessionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static com.atmin.saber.util.CyberColors.*;

public final class AuthMenu {

    private AuthMenu() {
    }

    public record LoginInput(String username, String password) {
    }

    public record RegisterInput(String username, String password, String phone, String fullname) {
    }

    public static void showLogo() {
        System.out.println(B_GRAD_1 + BOLD + "      ██████╗██╗   ██╗██████╗ ███████╗██████╗ ");
        System.out.println(B_GRAD_2 + BOLD + "     ██╔════╝╚██╗ ██╔╝██╔══██╗██╔════╝██╔══██╗");
        System.out.println(B_GRAD_3 + BOLD + "     ██║      ╚████╔╝ ██████╔╝█████╗  ██████╔╝");
        System.out.println(B_GRAD_4 + BOLD + "     ██║       ╚██╔╝  ██╔══██╗██╔══╝  ██╔══██╗");
        System.out.println(B_GRAD_5 + BOLD + "     ╚██████╗   ██║   ██████╔╝███████╗██║  ██║");
        System.out.println(B_GRAD_6 + BOLD + "      ╚═════╝   ╚═╝   ╚═════╝ ╚══════╝╚═╝  ╚═╝" + RESET);
        System.out.println(YELLOW + "CENTER MANAGEMENT SYSTEM v1.5 | PTIT B24 | OWNER: ATMIN" + RESET);
    }

    public static int showMenu(Scanner sc) {
        System.out.println("\t1. Login");
        System.out.println("\t2. Register");
        System.out.println("\t3. Continue as guest");
        System.out.println("\t0. Exit");
        System.out.print(GREEN + "  ➤ Your choice: " + RESET);
        return readInt(sc);
    }

    public static LoginInput promptLogin(Scanner sc) {
        System.out.println(CYAN + BOLD + "\n  === LOGIN ===" + RESET);
        String username = ConsoleInput.readNonEmpty(sc, "\tUsername: ", "\tUsername cannot be empty.");
        String password = ConsoleInput.readNonEmpty(sc, "\tPassword: ", "\tPassword cannot be empty.");
        return new LoginInput(username, password);
    }

    public static RegisterInput promptRegister(Scanner sc) {
        System.out.println(CYAN + BOLD + "\n  === REGISTER ===" + RESET);
        String username = ConsoleInput.readNonEmpty(sc, "\tUsername: ", "\tUsername cannot be empty.");
        String password = ConsoleInput.readNonEmpty(sc, "\tPassword (>=6 chars): ", "\tPassword cannot be empty.");
        String phone = ConsoleInput.readNonEmpty(sc, "\tPhone: ", "\tPhone cannot be empty.");
        String fullname = ConsoleInput.readNonEmpty(sc, "\tFull name: ", "\tFull name cannot be empty.");

        return new RegisterInput(username, password, phone, fullname);
    }

    /**
     * Select role for a user having multiple roles.
     * Keeps current API expectation from AuthController: this method will set SessionContext currentRole.
     */
    public static void roleMenu(Scanner sc, User user) {
        if (user == null) {
            SessionContext.clearCurrentRole();
            return;
        }

        Set<UserRole> roleSet = user.getRoles();
        if (roleSet == null || roleSet.isEmpty()) {
            SessionContext.clearCurrentRole();
            return;
        }

        List<UserRole> roles = new ArrayList<>(roleSet);
        roles.sort(Comparator.naturalOrder()); // ADMIN, STAFF, CUSTOMER

        while (true) {
            System.out.println(CYAN + BOLD + "\n  === SELECT ROLE ===" + RESET);
            for (int i = 0; i < roles.size(); i++) {
                System.out.printf("\t%d. %s%n", i + 1, roles.get(i));
            }
            System.out.println("\t0. Cancel");
            System.out.print(GREEN + "  ➤ Your choice: " + RESET);

            int choice = readInt(sc);
            if (choice == 0) {
                SessionContext.clearCurrentRole();
                return;
            }
            if (choice >= 1 && choice <= roles.size()) {
                SessionContext.setCurrentRole(roles.get(choice - 1));
                return;
            }
            System.out.println("\tInvalid choice. Please try again.");
        }
    }

    public static void pause(Scanner sc) {
        ConsoleInput.pressEnterToContinue(sc);
    }

    private static int readInt(Scanner sc) {
        while (true) {
            String raw = ConsoleInput.readLineOrThrow(sc).trim();
            if (raw.isEmpty()) {
                System.out.print(GREEN + "  ➤ Your choice: " + RESET);
                continue;
            }
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                System.out.println("\tPlease enter a valid number.");
                System.out.print(GREEN + "  ➤ Your choice: " + RESET);
            }
        }
    }


}

