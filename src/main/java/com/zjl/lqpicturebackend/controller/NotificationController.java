package com.zjl.lqpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjl.lqpicturebackend.common.BaseResponse;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.common.PageRequest;
import com.zjl.lqpicturebackend.common.ResultUtils;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.vo.NotificationVO;
import com.zjl.lqpicturebackend.service.NotificationService;
import com.zjl.lqpicturebackend.service.UserService;
import com.zjl.lqpicturebackend.utils.SseEmitterServer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/notify")
public class NotificationController {

    @Resource
    private UserService userService;
    @Resource
    private NotificationService notificationService;
    @Resource
    private SseEmitterServer sseEmitterServer;

    @GetMapping("/subscribe")
    public SseEmitter subscribe(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            // 在实际应用中，这里应该通过异常处理器返回一个标准的错误响应
            // 为了简单起见，这里返回 null，前端会收到一个错误
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return sseEmitterServer.createSse(loginUser.getId());
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<NotificationVO>> listNotify(@RequestBody PageRequest pageRequest, HttpServletRequest request) {
        long current = pageRequest.getCurrent();
        long size = pageRequest.getPageSize();
        User loginUser = userService.getLoginUser(request);
        Page<NotificationVO> page = notificationService.listMyNotifications(current, size, loginUser);
        return ResultUtils.success(page);
    }

    @PostMapping("/read/{id}")
    public BaseResponse<Boolean> markReadNotify(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        notificationService.markRead(id, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/read/all")
    public BaseResponse<Boolean> markAllReadNotify(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        notificationService.markAllRead(loginUser);
        return ResultUtils.success(true);
    }
}