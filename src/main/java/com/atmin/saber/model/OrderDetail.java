package com.atmin.saber.model;

import java.math.BigDecimal;

public class OrderDetail {
    private String detailId;
    private String orderId;
    private Integer id;
    private Integer quantity;
    private BigDecimal unitPrice;

    public OrderDetail() {
    }

    public OrderDetail(String detailId, String orderId, Integer id, Integer quantity, BigDecimal unitPrice) {
        this.detailId = detailId;
        this.orderId = orderId;
        this.id = id;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getDetailId() {
        return detailId;
    }

    public void setDetailId(String detailId) {
        this.detailId = detailId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

