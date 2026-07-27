package com.example.mcp.vo;

import java.io.Serializable;

import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 订单商品列表查询入参
 */
public class OrderItemsQueryParam implements Serializable {
    private static final long serialVersionUID = 1L;

    // 页码，默认1
    @ToolParam(description = "页码", required = false)
    private Integer page = 1;
    // 每页大小，默认10
    @ToolParam(description = "每页大小", required = false)
    private Integer size = 10;
    // 订单号（必选）
    @ToolParam(description = "订单号", required = true)
    private String orderNo;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

}