package com.atmin.saber.model;

import java.math.BigDecimal;

public class OrderDetail {
    private int detailId;
    private int orderId;
    private int id;
    private Integer quantity;
    private BigDecimal unitPrice;

    public OrderDetail() {
    }

    public void setDetailId(int detailId) {
        this.detailId = detailId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    @Override
    public String toString() {
        return "OrderDetail{" +
                "detailId=" + detailId +
                ", orderId=" + orderId +
                ", id=" + id +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                '}';
    }
}
