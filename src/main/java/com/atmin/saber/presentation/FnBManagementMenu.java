package com.atmin.saber.presentation;

import com.atmin.saber.controller.ProductController;
import com.atmin.saber.model.Product;
import com.atmin.saber.model.enums.ProductCategory;
import com.atmin.saber.util.ConsoleInput;

import java.math.BigDecimal;
import java.util.Scanner;

import static com.atmin.saber.util.CyberColors.*;

public class FnBManagementMenu {
    private static ProductController productController;

    public record AddProductInput(String productName,
                                  String description,
                                  BigDecimal price,
                                  int stockQuantity,
                                  ProductCategory category) {
    }

    public record EditProductInput(int productId,
                                   String newName,
                                   String newDescription,
                                   BigDecimal newPrice,
                                   Integer newStockQuantity,
                                   ProductCategory newCategory) {
    }

    public record DeleteProductInput(int productId, String confirm) {
    }

    private FnBManagementMenu() {
    }

    public static void start(Scanner scanner) {
        if (productController == null) {
            productController = ProductController.createDefault();
        }

        while (true) {
            showMainMenu();
            int choice = ConsoleInput.readInt(scanner, GREEN + "  ➤ Select an option: " + RESET, "\tPlease enter a valid number.");

            switch (choice) {
                case 1 -> {
                    productController.listProducts();
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case 2 -> {
                    AddProductInput addInput = promptAddProduct(scanner);
                    productController.addProduct(addInput);
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case 3 -> {
                    EditProductInput editInput = promptEditProduct(scanner);
                    if (editInput != null) {
                        productController.editProduct(editInput);
                    }
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case 4 -> {
                    DeleteProductInput deleteInput = promptDeleteProduct(scanner);
                    if (deleteInput != null) {
                        productController.deleteProduct(deleteInput);
                    }
                    ConsoleInput.pressEnterToContinue(scanner);
                }
                case 0 -> {
                    System.out.println(YELLOW + "\tReturning to Admin Panel..." + RESET);
                    return;
                }
                default -> System.out.println(YELLOW + "\tInvalid option. Please try again." + RESET);
            }
        }
    }

    private static void showMainMenu() {
        System.out.println(CYAN + BOLD + "\n  === FOOD & BEVERAGE MANAGEMENT ===" + RESET);
        System.out.println("\t1. View Product List");
        System.out.println("\t2. Add New Product");
        System.out.println("\t3. Edit Product Info");
        System.out.println("\t4. Delete Product");
        System.out.println("\t0. Back to Admin Panel");
    }

    private static AddProductInput promptAddProduct(Scanner scanner) {
        System.out.println(CYAN + BOLD + "\n  === ADD NEW PRODUCT ===" + RESET);
        String productName = promptNonBlank(scanner, "Product Name: ");
        String description = promptNonBlank(scanner, "Description: ");
        BigDecimal price = promptPrice(scanner, "Price: ", true);
        int stockQuantity = promptStock(scanner);
        ProductCategory category = promptCategory(scanner, "Category: ", true);
        return new AddProductInput(productName, description, price, stockQuantity, category);
    }

    private static EditProductInput promptEditProduct(Scanner scanner) {
        System.out.println(CYAN + BOLD + "\n  === EDIT PRODUCT INFO ===" + RESET);
        int productId = promptId(scanner, "Enter Product ID to edit: ");
        Product current = productController.getProductService().getById(productId).orElse(null);
        if (current == null) {
            System.out.println("\tProduct ID does not exist.");
            return null;
        }
        System.out.println("\n\tCurrent product information:");
        ProductController.printProductAsTable(current);
        System.out.println("\nLeave blank to keep current value.");
        return new EditProductInput(productId,
                promptOptionalText(scanner, "New Product Name: "),
                promptOptionalText(scanner, "New Description: "),
                promptPrice(scanner, "New Price: ", false),
                promptOptionalStock(scanner),
                promptCategory(scanner, "New Category: ", false));
    }

    private static DeleteProductInput promptDeleteProduct(Scanner scanner) {
        System.out.println(CYAN + BOLD + "\n  === DELETE PRODUCT ===" + RESET);
        int productId = promptId(scanner, "Enter Product ID to delete: ");
        Product current = productController.getProductService().getById(productId).orElse(null);
        if (current == null) {
            System.out.println("\tProduct ID does not exist.");
            return null;
        }
        System.out.println("\n\tYou are about to delete this product:");
        ProductController.printProductAsTable(current);
        System.out.println();
        return new DeleteProductInput(productId, promptConfirmation(scanner));
    }

    private static String promptNonBlank(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("\tThis field is required.");
        }
    }

    private static String promptOptionalText(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static BigDecimal promptPrice(Scanner scanner, String prompt, boolean required) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() && !required) return null;
            try {
                BigDecimal price = new BigDecimal(input);
                if (price.compareTo(BigDecimal.ZERO) > 0) return price;
                System.out.println("\tPrice must be greater than 0.");
            } catch (Exception e) {
                System.out.println("\tInvalid price format.");
            }
        }
    }

    private static int promptStock(Scanner scanner) {
        while (true) {
            System.out.print("Stock Quantity: ");
            try {
                int stock = Integer.parseInt(scanner.nextLine().trim());
                if (stock >= 0) return stock;
                System.out.println("\tStock quantity cannot be negative.");
            } catch (Exception e) {
                System.out.println("\tInvalid quantity format.");
            }
        }
    }

    private static Integer promptOptionalStock(Scanner scanner) {
        while (true) {
            System.out.print("New Stock Quantity: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return null;
            try {
                int stock = Integer.parseInt(input);
                if (stock >= 0) return stock;
                System.out.println("\tStock quantity cannot be negative.");
            } catch (Exception e) {
                System.out.println("\tInvalid quantity format.");
            }
        }
    }

    private static ProductCategory promptCategory(Scanner scanner, String prompt, boolean required) {
        System.out.println("Available categories:");
        System.out.println("\t1. FOOD");
        System.out.println("\t2. DRINK");
        System.out.println("\t3. CARD");
        if (!required) System.out.println("\t0. Keep current");
        System.out.print(prompt);
        String choice = promptMenuChoice(scanner);
        if (choice.isEmpty() && !required) return null;
        return switch (choice) {
            case "1" -> ProductCategory.FOOD;
            case "2" -> ProductCategory.DRINK;
            case "3" -> ProductCategory.CARD;
            case "0" -> required ? promptCategory(scanner, prompt, true) : null;
            default -> {
                System.out.println("\tInvalid choice. Please select 1, 2, or 3.");
                yield promptCategory(scanner, prompt, required);
            }
        };
    }

    private static int promptId(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int id = Integer.parseInt(scanner.nextLine().trim());
                if (id > 0) return id;
                System.out.println("\tID must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("\tInvalid ID format.");
            }
        }
    }

    private static String promptConfirmation(Scanner scanner) {
        while (true) {
            System.out.print("Are you sure you want to delete this product? (Y/N): ");
            String confirm = scanner.nextLine().trim().toUpperCase();
            if (confirm.equals("Y") || confirm.equals("N")) return confirm;
            System.out.println(YELLOW + "\tPlease enter Y or N.");
        }
    }

    private static String promptMenuChoice(Scanner scanner) {
        String choice = scanner.nextLine().trim();
        return choice.isEmpty() ? "-1" : choice;
    }
}