package com.example.mcp.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单详情返回结果
 */
public class OrderDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 订单号
    private String orderNo;
    // 订单金额
    private BigDecimal orderAmount;
    // 订单状态
    private String orderStatus;
    // 创建时间
    private String createTime;
    // 手机号
    private String phone;
    // 收货地址
    private String address;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}