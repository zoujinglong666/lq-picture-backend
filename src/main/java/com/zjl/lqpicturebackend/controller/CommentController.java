package com.zjl.lqpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjl.lqpicturebackend.common.BaseResponse;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.common.ResultUtils;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.model.Picture;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.dto.comment.CommentAddRequest;
import com.zjl.lqpicturebackend.model.dto.comment.CommentDeleteRequest;
import com.zjl.lqpicturebackend.model.vo.CommentVO;
import com.zjl.lqpicturebackend.service.CommentService;
import com.zjl.lqpicturebackend.service.NotificationService;
import com.zjl.lqpicturebackend.service.PictureService;
import com.zjl.lqpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private CommentService commentService;
    @Resource
    private NotificationService notificationService;

    @PostMapping("/{pictureId}/add")
    public BaseResponse<Long> add(@PathVariable Long pictureId, @RequestBody CommentAddRequest req, HttpServletRequest request) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 私有空间鉴权：只有空间所有者可操作
        if (picture.getSpaceId() != null) {
            pictureService.checkPictureAuth(loginUser, picture);
        }
        Long id = commentService.addComment(pictureId, req.getContent(), req.getParentId(), loginUser);
        if (!loginUser.getId().equals(picture.getUserId())) {
            notificationService.createForComment(pictureId, id, loginUser.getId());
        }
        return ResultUtils.success(id);
    }

    @GetMapping("/{pictureId}/list")
    public BaseResponse<Page<CommentVO>> list(@PathVariable Long pictureId,
                                                @RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size) {
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        return ResultUtils.success(commentService.listComments(pictureId, current, size));
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody CommentDeleteRequest req, HttpServletRequest request) {
        if (req == null || req.getId() == null || req.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = commentService.deleteComment(req.getId(), loginUser);
        return ResultUtils.success(result);
    }
}