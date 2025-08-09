package com.zjl.lqpicturebackend.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjl.lqpicturebackend.model.Space;
import com.zjl.lqpicturebackend.model.dto.space.SpaceAddRequest;
import com.zjl.lqpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zjl.lqpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author zou
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-07-31 21:11:36
 */
public interface SpaceService extends IService<Space> {

    /**
     * @param spaceAddRequest
     * @param request
     * @return 创建空间成功返回true，否则返回false
     */


    Long createSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request);

    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间
     *
     * @param space
     */
    void validSpace(Space space);
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);


    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);
}
