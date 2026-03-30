package com.atmin.saber.service.impl;

import com.atmin.saber.dao.ProductDao;
import com.atmin.saber.model.Product;
import com.atmin.saber.service.ProductService;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class ProductServiceImpl implements ProductService {

    private final ProductDao productDao;

    public ProductServiceImpl(ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    public List<Product> getAll() {
        return productDao.findAll();
    }

    @Override
    public Optional<Product> getById(int id) {
        return productDao.findById(id);
    }

    @Override
    public boolean existsById(int id) {
        return productDao.existsById(id);
    }

    @Override
    public void add(Product product) {
        productDao.insert(product);
    }

    @Override
    public void update(Product product) {
        productDao.update(product);
    }

    @Override
    public void delete(int id) {
        productDao.delete(id);
    }

    @Override
    public boolean decreaseStockIfEnough(int productId, int quantity) {
        return productDao.decreaseStockIfEnough(productId, quantity);
    }

    @Override
    public boolean decreaseStockIfEnough(Connection con, int productId, int quantity) {
        return productDao.decreaseStockIfEnough(con, productId, quantity);
    }
}
