package com.atmin.saber.service;

import com.atmin.saber.model.Product;

import java.util.List;
import java.util.Optional;
import java.sql.Connection;

public interface ProductService {
    List<Product> getAll();

    Optional<Product> getById(int id);

    void add(Product product);

    void update(Product product);

    void delete(int id);

    boolean decreaseStockIfEnough(Connection con, int productId, int quantity);
}
