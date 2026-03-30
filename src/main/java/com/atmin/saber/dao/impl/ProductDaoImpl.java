package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.ProductDao;
import com.atmin.saber.model.Product;
import com.atmin.saber.model.enums.ProductCategory;
import com.atmin.saber.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDaoImpl implements ProductDao {

    private final DBConnection db;

    public ProductDaoImpl(DBConnection db) {
        this.db = db;
    }

    @Override
    public List<Product> findAll() {
        String sql = "SELECT id, product_name, description, price, stock_quantity, category FROM products ORDER BY id DESC";
        List<Product> products = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load products: " + e.getMessage(), e);
        }
        return products;
    }

    @Override
    public Optional<Product> findById(int id) {
        String sql = "SELECT id, product_name, description, price, stock_quantity, category FROM products WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapProduct(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(int id) {
        String sql = "SELECT 1 FROM products WHERE id = ? LIMIT 1";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check product existence: " + e.getMessage(), e);
        }
    }

    @Override
    public void insert(Product product) {
        String sql = "INSERT INTO products(product_name, description, price, stock_quantity, category) VALUES(?, ?, ?, ?, ?)";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, product.getProductName());
            ps.setString(2, product.getDescription());
            ps.setBigDecimal(3, product.getPrice());
            ps.setInt(4, product.getStockQuantity());
            ps.setString(5, product.getCategory().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert product: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Product product) {
        String sql = "UPDATE products SET product_name = ?, description = ?, price = ?, stock_quantity = ?, category = ? WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, product.getProductName());
            ps.setString(2, product.getDescription());
            ps.setBigDecimal(3, product.getPrice());
            ps.setInt(4, product.getStockQuantity());
            ps.setString(5, product.getCategory().name());
            ps.setInt(6, product.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete product: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean decreaseStockIfEnough(int productId, int quantity) {
        try (Connection con = db.getConnection()) {
            return decreaseStockIfEnough(con, productId, quantity);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to decrease stock: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean decreaseStockIfEnough(Connection con, int productId, int quantity) {
        if (quantity <= 0) return true;

        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ? AND stock_quantity >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to decrease stock: " + e.getMessage(), e);
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getInt("id"));
        product.setProductName(rs.getString("product_name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setStockQuantity(rs.getInt("stock_quantity"));

        String categoryStr = rs.getString("category");
        try {
            product.setCategory(ProductCategory.valueOf(categoryStr == null ? "" : categoryStr.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            // Defensive: unknown category in DB. Do not crash the app.
            product.setCategory(ProductCategory.FOOD);
        }
        return product;
    }
}
