package com.zjl.lqpicturebackend.controller;

import com.zjl.lqpicturebackend.common.BaseResponse;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.common.ResultUtils;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.model.Picture;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.dto.like.PictureLikeRequest;
import com.zjl.lqpicturebackend.model.dto.user.UserSendCodeRequest;
import com.zjl.lqpicturebackend.service.LikeService;
import com.zjl.lqpicturebackend.service.NotificationService;
import com.zjl.lqpicturebackend.service.PictureService;
import com.zjl.lqpicturebackend.service.UserService;
import com.zjl.lqpicturebackend.service.impl.PictureLikeResponse;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/like")
public class LikeController {

    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private LikeService likeService;
    @Resource
    private NotificationService notificationService;

    @PostMapping("/picture/toggle")
    public BaseResponse<PictureLikeResponse> toggle(@RequestBody PictureLikeRequest pictureLikeRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long pictureId = pictureLikeRequest.getPictureId();
        if (pictureId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片id不能为空");
        }
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 私有空间鉴权：只有空间所有者可操作
        if (picture.getSpaceId() != null) {
            pictureService.checkPictureAuth(loginUser, picture);
        }
        PictureLikeResponse pictureLikeResponse = likeService.toggleLike(pictureId, loginUser);
        if (pictureLikeResponse.isLiked() && !loginUser.getId().equals(picture.getUserId())) {
            notificationService.createForLike(pictureId, loginUser.getId());
        }
        return ResultUtils.success(pictureLikeResponse);
    }

    @GetMapping("/{pictureId}/count")
    public BaseResponse<Long> count(@PathVariable Long pictureId) {
        return ResultUtils.success(likeService.countByPictureId(pictureId));
    }

    @GetMapping("/{pictureId}/me")
    public BaseResponse<Boolean> hasLiked(@PathVariable Long pictureId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(likeService.hasLiked(pictureId, loginUser == null ? null : loginUser.getId()));
    }
}