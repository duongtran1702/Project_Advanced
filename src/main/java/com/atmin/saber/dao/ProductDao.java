package com.atmin.saber.dao;

import com.atmin.saber.model.Product;

import java.util.List;
import java.util.Optional;
import java.sql.Connection;

public interface ProductDao {
    List<Product> findAll();

    Optional<Product> findById(int id);

    void insert(Product product);

    void update(Product product);

    void delete(int id);

    boolean decreaseStockIfEnough(Connection con, int productId, int quantity);
}
