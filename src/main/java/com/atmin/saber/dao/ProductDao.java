package com.atmin.saber.dao;

import com.atmin.saber.model.Product;

import java.util.List;
import java.util.Optional;
import java.sql.Connection;

public interface ProductDao {
    List<Product> findAll();

    Optional<Product> findById(int id);

    boolean existsById(int id);

    void insert(Product product);

    void update(Product product);

    void delete(int id);

    /**
     * Atomically decrease stock if enough in DB.
     * @return true if stock was decreased, false if not enough stock (or product missing)
     */
    boolean decreaseStockIfEnough(int productId, int quantity);

    /** Same as {@link #decreaseStockIfEnough(int, int)} but participates in an existing transaction. */
    boolean decreaseStockIfEnough(Connection con, int productId, int quantity);
}
