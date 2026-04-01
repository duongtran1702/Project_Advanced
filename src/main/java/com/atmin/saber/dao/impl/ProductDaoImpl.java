package com.atmin.saber.dao.impl;

import com.atmin.saber.dao.ProductDao;
import com.atmin.saber.dao.base.BaseDao;
import com.atmin.saber.dao.base.SqlConstants;
import com.atmin.saber.model.Product;
import com.atmin.saber.model.enums.ProductCategory;
import com.atmin.saber.util.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDaoImpl extends BaseDao implements ProductDao {

    public ProductDaoImpl(DBConnection db) {
        super(db);
    }

    @Override
    public List<Product> findAll() {
        String sql = "SELECT " + SqlConstants.PRODUCT_COLUMNS + " FROM products ORDER BY id DESC";
        return executeQuery(sql, rs -> {
            List<Product> products = new ArrayList<>();
            while (rs.next()) {
                products.add(mapProduct(rs));
            }
            return products;
        });
    }

    @Override
    public Optional<Product> findById(int id) {
        String sql = "SELECT " + SqlConstants.PRODUCT_COLUMNS + " FROM products WHERE id = ?";
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return Optional.of(mapProduct(rs));
            }
            return Optional.empty();
        }, id);
    }

    @Override
    public void insert(Product product) {
        String sql = "INSERT INTO products(product_name, description, price, stock_quantity, category) VALUES(?, ?, ?, ?, ?)";
        executeUpdate(sql, product.getProductName(), product.getDescription(), product.getPrice(),
                    product.getStockQuantity(), product.getCategory().name());
    }

    @Override
    public void update(Product product) {
        String sql = "UPDATE products SET product_name = ?, description = ?, price = ?, stock_quantity = ?, category = ? WHERE id = ?";
        executeUpdate(sql, product.getProductName(), product.getDescription(), product.getPrice(),
                    product.getStockQuantity(), product.getCategory().name(), product.getId());
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        executeUpdate(sql, id);
    }

    @Override
    public boolean decreaseStockIfEnough(Connection con, int productId, int quantity) {
        if (quantity <= 0) return true;

        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ? AND stock_quantity >= ?";
        try {
            return executeUpdate(con, sql, quantity, productId, quantity) == 1;
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
