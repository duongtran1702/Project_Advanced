package com.atmin.saber.controller;

import com.atmin.saber.dao.impl.BookingDaoImpl;
import com.atmin.saber.dao.impl.OrderDaoImpl;
import com.atmin.saber.dao.impl.PcDaoImpl;
import com.atmin.saber.model.Booking;
import com.atmin.saber.model.PC;
import com.atmin.saber.model.Order;
import com.atmin.saber.model.OrderDetail;
import com.atmin.saber.model.Product;
import com.atmin.saber.service.BookingService;
import com.atmin.saber.service.OrderService;
import com.atmin.saber.service.ProductService;
import com.atmin.saber.service.SessionBillingService;
import com.atmin.saber.service.impl.BookingServiceImpl;
import com.atmin.saber.service.impl.OrderServiceImpl;
import com.atmin.saber.service.impl.ProductServiceImpl;
import com.atmin.saber.service.impl.SessionBillingServiceImpl;
import com.atmin.saber.service.impl.WalletServiceImpl;
import com.atmin.saber.util.DBConnection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static com.atmin.saber.controller.PcController.safeShort;

public class BookingController {
    private final BookingService bookingService;
    private final ProductService productService;
    private final OrderService orderService;
    private final SessionBillingService sessionBillingService;
    private final Scanner scanner;

    public record BookingInput(int pcId, LocalDateTime startTime, LocalDateTime endTime) {
    }

    public record OrderInput(Map<Integer, Integer> items) {
    }
    
    public BookingController(BookingService bookingService, ProductService productService, OrderService orderService, SessionBillingService sessionBillingService) {
        this.bookingService = bookingService;
        this.productService = productService;
        this.orderService = orderService;
        this.sessionBillingService = sessionBillingService;
        this.scanner = new Scanner(System.in);
    }
    
    public static BookingController createDefault() {
        DBConnection db = DBConnection.getInstance();
        BookingService bookingService = new BookingServiceImpl(new BookingDaoImpl(db), new PcDaoImpl(db));
        ProductService productService = new ProductServiceImpl(new com.atmin.saber.dao.impl.ProductDaoImpl(db));
        OrderService orderService = new OrderServiceImpl(new OrderDaoImpl(db), productService);

        com.atmin.saber.dao.UserDao userDao = new com.atmin.saber.dao.impl.UserDaoImpl(db);
        com.atmin.saber.dao.TransactionDao txDao = new com.atmin.saber.dao.impl.TransactionDaoImpl(db);
        com.atmin.saber.service.WalletService walletService = new WalletServiceImpl(userDao, txDao, db);
        SessionBillingService sessionBillingService = new SessionBillingServiceImpl(new BookingDaoImpl(db), new PcDaoImpl(db), walletService);

        return new BookingController(bookingService, productService, orderService, sessionBillingService);
    }
    
    public void showAvailablePCsByZone() {
        System.out.println("\n=== AVAILABLE PCS BY ZONE ===");
        
        Map<String, List<PC>> pcsByZone = bookingService.getAvailablePCsGroupedByZone();
        
        if (pcsByZone.isEmpty()) {
            System.out.println("No PCs available currently.");
            return;
        }
        
        for (Map.Entry<String, List<PC>> entry : pcsByZone.entrySet()) {
            System.out.println("\n" + entry.getKey().toUpperCase() + ":");
            System.out.println("+------+----------------------+--------------+");
            System.out.printf("| %-4s | %-20s | %-12s |%n", "ID", "NAME", "STATUS");
            System.out.println("+------+----------------------+--------------+");
            
            for (PC pc : entry.getValue()) {
                System.out.printf("| %-4d | %-20s | %-12s |%n",
                    pc.getPcId(), pc.getPcName(), "Available");
            }
            System.out.println("+------+----------------------+--------------+");
        }
    }
    
    public void makeBooking(String customerId) {
        System.out.println("\n=== BOOK A PC ===");

        Map<Integer, PC> availableMap = showAvailablePCsByZoneAndReturnMap();
        if (availableMap.isEmpty()) {
            System.out.println("No PCs available currently.");
            return;
        }

        BookingInput bookingInput = promptBookingInput(availableMap);
        if (bookingInput == null) return;

        try {
            if (!bookingService.isPcAvailable(bookingInput.pcId(), bookingInput.startTime(), bookingInput.endTime())) {
                System.out.println("Error: PC is already booked for this time slot!");
                return;
            }

            Booking booking = bookingService.createBooking(customerId, bookingInput.pcId(), bookingInput.startTime(), bookingInput.endTime());

            OrderService.CreatedOrder createdOrder = null;
            if (promptOrderYesNo()) {
                OrderInput orderInput = promptOrderInput();
                if (orderInput != null && !orderInput.items().isEmpty()) {
                    createdOrder = orderService.createOrderForBooking(customerId, booking, orderInput.items());
                }
            }

            System.out.println("\n[SUCCESS] BOOKING CREATED! Your information has been saved to the system.");
            printExpectedBill(booking, createdOrder);

        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Show available PCs grouped by zone and return a map for validation.
     */
    private Map<Integer, PC> showAvailablePCsByZoneAndReturnMap() {
        showAvailablePCsByZone();
        Map<Integer, PC> map = new HashMap<>();
        bookingService.getAvailablePCsByZone().forEach(pc -> map.put(pc.getPcId(), pc));
        return map;
    }
    
    public List<Booking> getCustomerBookings(String customerId) {
        return bookingService.getCustomerBookings(customerId);
    }
    
    public void cancelBooking(String customerId, int pcId, LocalDateTime startTime) {
        bookingService.cancelBooking(customerId, pcId, startTime);
    }

    public void startBooking(String customerId, int pcId, LocalDateTime startTime) {
        sessionBillingService.startBooking(customerId, pcId, startTime);
    }

    public java.util.Optional<java.math.BigDecimal> stopActiveBookingAndCharge(String customerId) {
        return sessionBillingService.stopActiveBookingAndCharge(customerId);
    }

    public java.util.Optional<Booking> getActiveBooking(String customerId) {
        return sessionBillingService.getActiveBookingOfCustomer(customerId);
    }
    
    private void printExpectedBill(Booking booking, OrderService.CreatedOrder createdOrder) {
        Order order = createdOrder == null ? null : createdOrder.order();
        List<OrderDetail> details = createdOrder == null ? List.of() : createdOrder.details();
        BigDecimal fnbTotal = order == null ? BigDecimal.ZERO : order.getTotalAmount();

        System.out.println("\n=== EXPECTED BILL DETAILS ===");
        System.out.println("BOOKING INFO:");
        System.out.println("- PC ID: " + booking.getPcId());
        System.out.println("- Start time: " + booking.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        System.out.println("- End time: " + booking.getExpectedEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        System.out.println("- PC fee: " + booking.getTotalFee() + " VND");
        System.out.println("- Booking status: " + formatBookingStatus(booking.getStatus()));

        if (fnbTotal.compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("\nF&B INFO:");
            System.out.println("- F&B total: " + fnbTotal + " VND");
            System.out.println("- Order status: " + (order == null || order.getStatus() == null ? "PENDING" : order.getStatus().name()));

            System.out.println("\nF&B DETAILS:");
            System.out.println("+------+----------------------+----------+---------------+---------------+");
            System.out.printf("| %-4s | %-20s | %-8s | %-13s | %-13s |%n", "ID", "PRODUCT NAME", "QUANTITY", "UNIT PRICE", "LINE TOTAL");
            System.out.println("+------+----------------------+----------+---------------+---------------+");
            for (OrderDetail d : details) {
                if (d == null || d.getId() == null) continue;
                Product p = productService.getById(d.getId()).orElse(null);
                String name = p == null ? "" : safeShort(p.getProductName());
                BigDecimal unit = d.getUnitPrice() == null ? BigDecimal.ZERO : d.getUnitPrice();
                int qty = d.getQuantity() == null ? 0 : d.getQuantity();
                BigDecimal line = unit.multiply(BigDecimal.valueOf(qty));
                System.out.printf("| %-4d | %-20s | %-8d | %-13s | %-13s |%n", d.getId(), name, qty, unit, line);
            }
            System.out.println("+------+----------------------+----------+---------------+---------------+");
        }

        BigDecimal grandTotal = booking.getTotalFee().add(fnbTotal);
        System.out.println("\nTOTAL BILL: " + grandTotal + " VND");
        System.out.println("Status: Pending confirmation");
    }
    
    private String formatBookingStatus(com.atmin.saber.model.enums.BookingStatus status) {
        return switch (status) {
            case PENDING -> "Pending confirmation";
            case ACTIVE -> "In use";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }
    

    private BookingInput promptBookingInput(Map<Integer, PC> availableMap) {
        int pcId = promptPcIdOrBack();
        if (pcId == 0) return null;
        if (!availableMap.containsKey(pcId)) {
            System.out.println("Error: Please choose a PC from the available list.");
            return null;
        }

        LocalDateTime startTime = promptTimeOrBack("Enter start time (dd/MM/yyyy HH:mm) (or 0 to return): ");
        if (startTime == null) return null;
        if (startTime.isBefore(LocalDateTime.now())) {
            System.out.println("Error: Start time cannot be in the past.");
            return null;
        }

        LocalDateTime endTime = promptTimeOrBack("Enter end time (dd/MM/yyyy HH:mm) (or 0 to return): ");
        if (endTime == null) return null;
        if (!endTime.isAfter(startTime)) {
            System.out.println("Error: End time must be after start time.");
            return null;
        }

        return new BookingInput(pcId, startTime, endTime);
    }

    private OrderInput promptOrderInput() {
        List<Product> products = productService.getAll();
        if (products.isEmpty()) {
            System.out.println("No products available in menu currently.");
            return null;
        }

        printProductMenu(products);

        Map<Integer, Integer> items = new HashMap<>();
        while (true) {
            int productId = promptProductIdAllowZero();
            if (productId == 0) break;

            Product product = productService.getById(productId).orElse(null);
            if (product == null) {
                System.out.println("Error: Product does not exist.");
                continue;
            }
            if (product.getStockQuantity() == null || product.getStockQuantity() <= 0) {
                System.out.println("Error: Product is out of stock.");
                continue;
            }

            int quantity = promptQuantity();
            if (quantity <= 0) {
                System.out.println("Error: Quantity must be greater than 0.");
                continue;
            }
            if (quantity > product.getStockQuantity()) {
                System.out.println("Error: Only " + product.getStockQuantity() + " products available in stock.");
                continue;
            }

            items.put(productId, items.getOrDefault(productId, 0) + quantity);
            System.out.println("[OK] Added " + quantity + " " + product.getProductName() + " to your order.");
        }

        return new OrderInput(items);
    }

    private void printProductMenu(List<Product> products) {
        System.out.println("\nF&B MENU:");
        System.out.println("+------+----------------------+----------------------+---------------+----------+");
        System.out.printf("| %-4s | %-20s | %-20s | %-13s | %-8s |%n", "ID", "NAME", "DESCRIPTION", "PRICE", "STOCK");
        System.out.println("+------+----------------------+----------------------+---------------+----------+");

        for (Product product : products) {
            System.out.printf("| %-4d | %-20s | %-20s | %-13s | %-8d |%n",
                    product.getId(),
                    safeShort(product.getProductName()),
                    safeShort(product.getDescription()),
                    product.getPrice() != null ? product.getPrice().toString() : "0.00",
                    product.getStockQuantity() == null ? 0 : product.getStockQuantity());
        }
        System.out.println("+------+----------------------+----------------------+---------------+----------+");
    }

    private int promptPcIdOrBack() {
        while (true) {
            System.out.print("\nEnter PC ID to book (0 to return): ");
            String raw = scanner.nextLine().trim();
            if (raw.equals("0")) return 0;
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("Error: input must be a number.");
            }
        }
    }

    private int promptQuantity() {
        while (true) {
            System.out.print("Enter quantity: ");
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Error: input must be a number.");
            }
        }
    }

    private int promptProductIdAllowZero() {
        while (true) {
            System.out.print("\nEnter product ID (0 to finish): ");
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= 0) return value;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Error: input must be a number (>= 0).");
        }
    }


    private LocalDateTime promptTimeOrBack(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.equals("0")) return null;
        try {
            return LocalDateTime.parse(input, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (DateTimeParseException e) {
            System.out.println("Error: Invalid time format. Please use dd/MM/yyyy HH:mm format");
            return null;
        }
    }

    private boolean promptOrderYesNo() {
        System.out.print("Do you want to order F&B? (Y/N): ");
        String s = scanner.nextLine().trim();
        return s.equalsIgnoreCase("Y");
    }

    public List<Booking> getPendingBookings() {
        return bookingService.getPendingBookings();
    }
}
