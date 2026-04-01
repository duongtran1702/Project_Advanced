package com.atmin.saber.presentation;

import com.atmin.saber.controller.BookingController;
import com.atmin.saber.service.OrderService;
import com.atmin.saber.service.WalletService;
import com.atmin.saber.util.DBConnection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Smoke tests to ensure menus do not spin / spam output when input reaches EOF.
 * These tests do not assert business outcomes; they only ensure termination.
 */
public class MenuEofSmokeTest {

    @Test
    void staffMenu_shouldReturnOnImmediateEof() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            Scanner sc = new Scanner(new ByteArrayInputStream(new byte[0]));
            StaffMenu staffMenu = StaffMenu.createDefault(sc);
            staffMenu.showMenu();
        });
    }

    @Test
    void customerMenu_shouldReturnOnImmediateEof() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            // Build CustomerMenu with real wiring, but feed EOF-only input.
            DBConnection db = DBConnection.getInstance();
            com.atmin.saber.service.ProductService productService = new com.atmin.saber.service.impl.ProductServiceImpl(
                    new com.atmin.saber.dao.impl.ProductDaoImpl(db)
            );
            com.atmin.saber.dao.UserDao userDao = new com.atmin.saber.dao.impl.UserDaoImpl(db);
            com.atmin.saber.dao.TransactionDao txDao = new com.atmin.saber.dao.impl.TransactionDaoImpl(db);
            WalletService walletService = new com.atmin.saber.service.impl.WalletServiceImpl(userDao, txDao, db);

            OrderService orderService = new com.atmin.saber.service.impl.OrderServiceImpl(
                    new com.atmin.saber.dao.impl.OrderDaoImpl(db),
                    new com.atmin.saber.dao.impl.OrderDetailDaoImpl(db),
                    productService,
                    walletService
            );

            Scanner sc = new Scanner(new ByteArrayInputStream(new byte[0]));
            CustomerMenu customerMenu = new CustomerMenu(BookingController.createDefault(), orderService, walletService, productService, sc);
            customerMenu.showMenu();
        });
    }

    @Test
    void staffMenu_shouldNotSpinOnInvalidThenEof() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            String script = "9\n"; // invalid choice then EOF
            Scanner sc = new Scanner(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
            StaffMenu staffMenu = StaffMenu.createDefault(sc);
            staffMenu.showMenu();
        });
    }

    @Test
    void customerMenu_shouldNotSpinOnInvalidThenEof() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            String script = "99\n"; // invalid choice then EOF

            DBConnection db = DBConnection.getInstance();
            com.atmin.saber.service.ProductService productService = new com.atmin.saber.service.impl.ProductServiceImpl(
                    new com.atmin.saber.dao.impl.ProductDaoImpl(db)
            );
            com.atmin.saber.dao.UserDao userDao = new com.atmin.saber.dao.impl.UserDaoImpl(db);
            com.atmin.saber.dao.TransactionDao txDao = new com.atmin.saber.dao.impl.TransactionDaoImpl(db);
            WalletService walletService = new com.atmin.saber.service.impl.WalletServiceImpl(userDao, txDao, db);

            OrderService orderService = new com.atmin.saber.service.impl.OrderServiceImpl(
                    new com.atmin.saber.dao.impl.OrderDaoImpl(db),
                    new com.atmin.saber.dao.impl.OrderDetailDaoImpl(db),
                    productService,
                    walletService
            );

            Scanner sc = new Scanner(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
            CustomerMenu customerMenu = new CustomerMenu(BookingController.createDefault(), orderService, walletService, productService, sc);
            customerMenu.showMenu();
        });
    }
}

