package com.atmin.saber.controller;

import com.atmin.saber.dao.impl.ProductDaoImpl;
import com.atmin.saber.model.Product;
import com.atmin.saber.presentation.FnBManagementMenu;
import com.atmin.saber.service.ProductService;
import com.atmin.saber.service.impl.ProductServiceImpl;
import com.atmin.saber.util.DBConnection;

import java.util.List;

import static com.atmin.saber.controller.PcController.safeShort;

@SuppressWarnings("ClassCanBeRecord")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    public static ProductController createDefault() {
        return new ProductController(new ProductServiceImpl(new ProductDaoImpl(DBConnection.getInstance())));
    }

    public ProductService getProductService() {
        return productService;
    }

    public void listProducts() {
        try {
            List<Product> products = productService.getAll();
            if (products.isEmpty()) {
                System.out.println("\tThe product list is currently empty.");
                return;
            }

            printProductListAsTable(products);

        } catch (RuntimeException ex) {
            System.out.println("\tFailed to load product list: " + ex.getMessage());
        }
    }

    public void addProduct(FnBManagementMenu.AddProductInput input) {
        try {
            if (input == null) {
                System.out.println("\tInvalid input for adding a product.");
                return;
            }

            Product product = new Product(0, input.productName(), input.description(), input.price(),
                    input.stockQuantity(), input.category());
            productService.add(product);
            System.out.println("\tProduct added successfully!");

        } catch (RuntimeException ex) {
            System.out.println("\tFailed to add product: " + ex.getMessage());
        }
    }

    public void editProduct(FnBManagementMenu.EditProductInput input) {
        try {
            if (input == null) {
                System.out.println("\tInvalid input for editing a product.");
                return;
            }

            Product oldProduct = productService.getById(input.productId()).orElse(null);
            if (oldProduct == null) {
                return;
            }

            boolean hasUpdate = !isBlank(input.newName())
                    || !isBlank(input.newDescription())
                    || input.newPrice() != null
                    || input.newStockQuantity() != null
                    || input.newCategory() != null;

            if (!hasUpdate) {
                System.out.println("\tNo fields were updated. Keeping old data.");
                return;
            }

            Product updated = new Product();
            updated.setId(oldProduct.getId());
            updated.setProductName(isBlank(input.newName()) ? oldProduct.getProductName() : input.newName().trim());
            updated.setDescription(
                    isBlank(input.newDescription()) ? oldProduct.getDescription() : input.newDescription().trim());
            updated.setPrice(input.newPrice() != null ? input.newPrice() : oldProduct.getPrice());
            updated.setStockQuantity(
                    input.newStockQuantity() != null ? input.newStockQuantity() : oldProduct.getStockQuantity());
            updated.setCategory(input.newCategory() != null ? input.newCategory() : oldProduct.getCategory());

            productService.update(updated);

            System.out.println("\tUpdated successfully!");

        } catch (RuntimeException ex) {
            System.out.println("\tFailed to update product: " + ex.getMessage());
        }
    }

    public void deleteProduct(FnBManagementMenu.DeleteProductInput input) {
        try {
            if (input == null) {
                System.out.println("\tInvalid input for deleting a product.");
                return;
            }

            Product product = productService.getById(input.productId()).orElse(null);
            if (product == null) {
                return;
            }

            if (!isYes(input.confirm())) {
                System.out.println("\tDelete canceled.");
                return;
            }

            productService.delete(input.productId());
            System.out.println("\tProduct deleted successfully!");

        } catch (RuntimeException ex) {
            System.out.println("\tFailed to delete product: " + ex.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isYes(String s) {
        return s != null && s.trim().equalsIgnoreCase("Y");
    }

    private static void printProductTableHeader() {
        System.out.println(
                "\n+------+----------------------+----------------------+---------------+--------------+----------+");
        System.out.printf("| %-4s | %-20s | %-20s | %-13s | %-12s | %-8s |%n", "ID", "NAME", "DESCRIPTION", "PRICE",
                "STOCK", "CATEGORY");
        System.out.println(
                "+------+----------------------+----------------------+---------------+--------------+----------+");
    }

    private static void printProductTableFooter() {
        System.out.println(
                "+------+----------------------+----------------------+---------------+--------------+----------+");
    }

    public static void printProductAsTable(Product product) {
        printProductTableHeader();
        printProductRow(product);
        printProductTableFooter();
    }

    public static void printProductListAsTable(List<Product> products) {
        if (products == null || products.isEmpty())
            return;
        printProductTableHeader();
        for (Product product : products) {
            printProductRow(product);
        }
        printProductTableFooter();
    }

    private static void printProductRow(Product product) {
        System.out.printf("| %-4d | %-20s | %-20s | %-13s | %-12d | %-8s |%n",
                product.getId(),
                safeShort(product.getProductName()),
                safeShort(product.getDescription()),
                product.getPrice() != null ? product.getPrice().toString() : "0.00",
                product.getStockQuantity() != null ? product.getStockQuantity() : 0,
                product.getCategory() != null ? product.getCategory().name() : "");
    }

}
