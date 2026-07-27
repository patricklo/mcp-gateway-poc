package com.example.mcp.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.mcp.controller.SassController;
import com.example.mcp.vo.OrderDetailQueryParam;
import com.example.mcp.vo.OrderDetailVO;
import com.example.mcp.vo.OrderListQueryParam;
import com.example.mcp.vo.OrderVO;
import com.example.mcp.vo.PageResult;

@McpTools
public class McpService {
    @Autowired
    SassController SassController;

    @Tool(description = "获取订单详情")
    public OrderDetailVO getOrderDetail(OrderDetailQueryParam param) {
        return SassController.getOrderDetail(param);
    }

    @Tool(description = "获取订单列表（分页）")
    public PageResult<OrderVO> getOrderList(OrderListQueryParam param) {
        return SassController.getOrderList(param);
    }

}
