package com.example.mcp.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单商品项返回结果
 */
public class OrderItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 商品ID
    private String itemId;
    // 商品名称
    private String itemName;
    // 购买数量
    private Integer quantity;
    // 商品单价
    private BigDecimal price;
    // 商品总价
    private BigDecimal totalPrice;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

}