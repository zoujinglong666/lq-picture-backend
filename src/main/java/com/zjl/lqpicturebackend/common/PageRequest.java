package com.zjl.lqpicturebackend.common;

import com.zjl.lqpicturebackend.constant.CommonConstant;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求
 */
@Data
@NoArgsConstructor  // ✅ 补上无参构造
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;

    /**
     * 兼容前端 page 字段
     */
    public void setPage(int page) {
        this.current = page;
    }
}
