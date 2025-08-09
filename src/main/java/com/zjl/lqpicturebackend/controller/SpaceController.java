package com.zjl.lqpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjl.lqpicturebackend.annotation.AuthCheck;
import com.zjl.lqpicturebackend.common.BaseResponse;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.common.ResultUtils;
import com.zjl.lqpicturebackend.constant.UserConstant;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.exception.ThrowUtils;
import com.zjl.lqpicturebackend.model.Space;
import com.zjl.lqpicturebackend.model.dto.space.SpaceAddRequest;
import com.zjl.lqpicturebackend.model.dto.space.SpaceLevel;
import com.zjl.lqpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zjl.lqpicturebackend.model.enums.SpaceLevelEnum;
import com.zjl.lqpicturebackend.model.vo.SpaceVO;
import com.zjl.lqpicturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zou
 */
@RestController
@Slf4j
@RequestMapping("/space")
public class SpaceController {


    @Resource
    private SpaceService spaceService;

    @PostMapping("/create")
    public BaseResponse<Long> createSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {

        if (spaceAddRequest == null || spaceAddRequest.getSpaceName() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        Long pictureSpace = this.spaceService.createSpace(spaceAddRequest, request);
        return ResultUtils.success(pictureSpace);
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = this.spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceByPageVo(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        // 查询数据库
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }

    /**
     * 获取空间级别列表，便于前端展示
     *
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(this::buildSpaceLevelFromEnum)
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

    private SpaceLevel buildSpaceLevelFromEnum(SpaceLevelEnum levelEnum) {
        return new SpaceLevel(
                levelEnum.getValue(),
                levelEnum.getText(),
                levelEnum.getMaxCount(),
                levelEnum.getMaxSize()
        );
    }



}
