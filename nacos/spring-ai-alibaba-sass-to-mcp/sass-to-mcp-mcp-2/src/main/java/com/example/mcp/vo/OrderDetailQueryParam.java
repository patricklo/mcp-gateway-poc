package com.example.mcp.vo;

import java.io.Serializable;

import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 订单详情查询入参
 */
public class OrderDetailQueryParam implements Serializable {
    private static final long serialVersionUID = 1L;

    // 订单号（必选）
    @ToolParam(description = "订单号", required = true)
    private String orderNo;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

}