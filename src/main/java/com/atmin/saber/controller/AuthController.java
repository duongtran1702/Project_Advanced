package com.atmin.saber.controller;

import com.atmin.saber.model.enums.UserRole;
import com.atmin.saber.presentation.AdminMenu;
import com.atmin.saber.presentation.AuthMenu;
import com.atmin.saber.presentation.CustomerMenu;
import com.atmin.saber.presentation.StaffMenu;

import com.atmin.saber.util.ConsoleInput;
import com.atmin.saber.util.SessionContext;
import com.atmin.saber.service.AuthService;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class AuthController {
    private final AuthService authService;
    private final Scanner sc;

    public AuthController(AuthService authService, Scanner sc) {
        this.authService = authService;
        this.sc = sc;
    }

    public void start() {
        while (true) {
            AuthMenu.showLogo();

            try {
                int choice = AuthMenu.showMenu(sc);
                switch (choice) {
                    case 1:
                        handleLogin(AuthMenu.promptLogin(sc));
                        break;
                    case 2:
                        handleRegister(AuthMenu.promptRegister(sc));
                        break;
                    case 3:
                        handleGuest();
                        break;
                    case 0:
                        System.out.println("Exit!");
                        return;
                    default:
                        AuthMenu.pause(sc);
                        break;
                }
            } catch (ConsoleInput.EndOfInputException | NoSuchElementException | IllegalStateException eof) {
                // Input stream ended (e.g., piped input finished). Exit gracefully.
                System.out.println("\n[EOF] No more input. Exiting...");
                return;
            } catch (RuntimeException ex) {
                System.out.println("\tAn error occurred: " + ex.getMessage());
                AuthMenu.pause(sc);
            }
        }
    }

    private boolean attemptLogin(String username, String password) {
        // Login using AuthService and create session
        return authService.login(username, password)
                .map(user -> {
                    //1. Set user
                    SessionContext.setCurrentUser(user);
                    //2. Set role
                    if (user.getRoles().size() > 1) {
                        AuthMenu.roleMenu(sc, user);
                    } else if (!user.getRoles().isEmpty()) {
                        SessionContext.setCurrentRole(user.getRoles().iterator().next());
                    }
                    //3. Check role is empty
                    if (SessionContext.getCurrentRole().isEmpty()) {
                        SessionContext.clearCurrentRole();
                        SessionContext.clearCurrentUser();
                        return false;
                    }

                    System.out.println("\tLogin Successful!");
                    return true;
                })
                .orElse(false);
    }

    private void dispatchByRole() {
        UserRole role = SessionContext.getCurrentRole().orElse(null);
        if (role == null) {
            return;
        }

        switch (role) {
            case ADMIN:
                AdminMenu.start(sc);
                break;
            case STAFF:
                StaffMenu staffMenu = StaffMenu.createDefault(sc);
                staffMenu.showMenu();
                break;
            case CUSTOMER:
                CustomerMenu customerMenu = CustomerMenu.createDefault(sc);
                customerMenu.showMenu();
                break;
        }
    }

    private void handleGuest() {
        // Guest has CUSTOMER role but no authenticated user.
        SessionContext.clearCurrentUser();
        SessionContext.setCurrentRole(UserRole.CUSTOMER);

        // Guest menu (restricted): reuse CustomerMenu but it will block actions requiring login.
        CustomerMenu customerMenu = CustomerMenu.createDefault(sc);
        customerMenu.showMenu();

        // Ensure we don't keep guest session when returning to Auth menu
        SessionContext.clearCurrentRole();
        SessionContext.clearCurrentUser();
    }

    private void handleLogin(AuthMenu.LoginInput loginInput) {
        String username = loginInput.username().trim();
        String password = loginInput.password().trim();

        try {
            if (attemptLogin(username, password)) {
                AuthMenu.pause(sc);
                dispatchByRole();
                return;
            }
        } catch (RuntimeException ex) {
            System.out.println("\tError: " + ex.getMessage());
        }

        System.out.println("\n\tLogin Failed!");
        // Skip the Try again/Return choice. Go back to the main menu immediately.
        AuthMenu.pause(sc);
    }

    private void handleRegister(AuthMenu.RegisterInput reg) {
        // Handle registration.
        // Data has been entered from AuthMenu; here we only check uniqueness (username/phone) and call AuthService.
        try {
            // Validate uniqueness at controller layer (DB)
            if (authService.existsUsername(reg.username())) {
                System.out.println("\tUsername already exists. Please choose another.");
                AuthMenu.pause(sc);
                return;
            }
            if (authService.existsPhone(reg.phone())) {
                System.out.println("\tPhone already exists. Please enter another.");
                AuthMenu.pause(sc);
                return;
            }

            if (authService.register(reg.username(), reg.password(), reg.phone(), reg.fullname()).isPresent()) {
                System.out.println("\tRegistration Successful!");
            } else {
                System.out.println("\tRegistration Failed! Please try again.");
            }
        } catch (RuntimeException ex) {
            System.out.println("\tRegistration error: " + ex.getMessage());
        }

        AuthMenu.pause(sc);
    }

}
