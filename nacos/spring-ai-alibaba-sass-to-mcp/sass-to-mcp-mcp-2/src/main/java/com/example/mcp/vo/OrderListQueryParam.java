package com.example.mcp.vo;

import java.io.Serializable;

import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 订单列表查询入参
 */
public class OrderListQueryParam implements Serializable {
    private static final long serialVersionUID = 1L;

    // 页码，默认1
    @ToolParam(description = "页码",required = false)
    private Integer page = 1;
    // 每页大小，默认10
    @ToolParam(description = "每页大小",required = false)
    private Integer size = 10;
    // 手机号（可选）
    @ToolParam(description = "手机号",required = false)
    private String phone;
    // 订单号（可选）
    @ToolParam(description = "订单号",required = false)
    private String orderNo;
    // 开始日期（可选）
    @ToolParam(description = "开始日期 格式：yyyy-MM-dd",required = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String startDate;
    // 结束日期（可选）
    @ToolParam(description = "结束日期 格式：yyyy-MM-dd",required = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String endDate;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

}