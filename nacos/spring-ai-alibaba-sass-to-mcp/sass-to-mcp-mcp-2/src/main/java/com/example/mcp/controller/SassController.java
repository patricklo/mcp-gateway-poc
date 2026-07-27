package com.example.mcp.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.mcp.vo.OrderDetailQueryParam;
import com.example.mcp.vo.OrderDetailVO;
import com.example.mcp.vo.OrderItemVO;
import com.example.mcp.vo.OrderItemsQueryParam;
import com.example.mcp.vo.OrderListQueryParam;
import com.example.mcp.vo.OrderVO;
import com.example.mcp.vo.PageResult;

@RestController
@RequestMapping("/order")
public class SassController {

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 订单详情接口
     *
     * @param param 订单号入参
     * @return 订单详情
     */
    @GetMapping("/detail")
    public OrderDetailVO getOrderDetail(OrderDetailQueryParam param) {
        String orderNo = param.getOrderNo();
        log.info("调用订单详情接口，订单号：{}", orderNo);

        OrderDetailVO orderDetail = new OrderDetailVO();
        orderDetail.setOrderNo(orderNo);
        orderDetail.setOrderAmount(new BigDecimal("199.99"));
        orderDetail.setOrderStatus("已支付");
        orderDetail.setCreateTime("2026-02-03 10:00:00");
        orderDetail.setPhone("13800138000");
        orderDetail.setAddress("北京市朝阳区");

        return orderDetail;
    }

    /**
     * 分页订单列表接口
     *
     * @param param 分页查询入参
     * @return 分页订单列表
     */
    @GetMapping("/list")
    public PageResult<OrderVO> getOrderList(OrderListQueryParam param) {
        Integer page = param.getPage();
        Integer size = param.getSize();
        String phone = param.getPhone();
        String orderNo = param.getOrderNo();
        String startDate = param.getStartDate();
        String endDate = param.getEndDate();

        log.info("调用订单列表接口，页码：{}，每页大小：{}，手机号：{}，订单号：{}，开始日期：{}，结束日期：{}", page, size, phone, orderNo, startDate, endDate);

        PageResult<OrderVO> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);
        result.setTotal(25);

        List<OrderVO> orders = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            OrderVO order = new OrderVO();
            order.setOrderNo("ORD" + (1000 + (page - 1) * size + i));
            order.setOrderAmount(new BigDecimal("199.99").add(new BigDecimal(i * 10)));
            order.setOrderStatus("已支付");
            order.setCreateTime("2026-02-03 10:00:00");
            order.setPhone(phone != null ? phone : "13800138000");
            orders.add(order);
        }
        result.setList(orders);

        return result;
    }

    /**
     * 分页订单商品列表
     *
     * @param param 分页查询入参
     * @return 分页订单商品列表
     */
    @GetMapping("/items")
    public PageResult<OrderItemVO> getOrderItems(OrderItemsQueryParam param) {
        Integer page = param.getPage();
        Integer size = param.getSize();
        String orderNo = param.getOrderNo();

        log.info("调用订单商品列表接口，页码：{}，每页大小：{}，订单号：{}", page, size, orderNo);

        PageResult<OrderItemVO> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);
        result.setTotal(5);

        List<OrderItemVO> items = new ArrayList<>();
        for (int i = 0; i < size && i < 5; i++) {
            OrderItemVO item = new OrderItemVO();
            item.setItemId("ITEM" + (1000 + i));
            item.setItemName("商品" + (i + 1));
            item.setQuantity(2);
            item.setPrice(new BigDecimal("99.99"));
            item.setTotalPrice(new BigDecimal("199.98"));
            items.add(item);
        }

        result.setList(items);
        return result;
    }
}