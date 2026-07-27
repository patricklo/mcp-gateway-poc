package com.example.mcp.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页返回结果
 * 
 * @param <T> 列表数据类型
 */
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // 页码
    private Integer page;
    // 每页大小
    private Integer size;
    // 总记录数
    private Integer total;
    // 分页列表数据
    private List<T> list;

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

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

}