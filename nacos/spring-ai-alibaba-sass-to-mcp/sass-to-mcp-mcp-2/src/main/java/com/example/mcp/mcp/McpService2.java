package com.example.mcp.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.mcp.controller.SassController;
import com.example.mcp.vo.OrderItemVO;
import com.example.mcp.vo.OrderItemsQueryParam;
import com.example.mcp.vo.PageResult;

@McpTools
public class McpService2 {
    @Autowired
    SassController SassController;

    @Tool(description = "获取订单商品列表（分页）")
    public PageResult<OrderItemVO> getOrderItems(OrderItemsQueryParam param) {
        return SassController.getOrderItems(param);
    }
}
