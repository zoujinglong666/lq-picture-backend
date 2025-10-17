package com.zjl.lqpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjl.lqpicturebackend.common.BaseResponse;
import com.zjl.lqpicturebackend.common.ResultUtils;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.vo.NotificationVO;
import com.zjl.lqpicturebackend.service.NotificationService;
import com.zjl.lqpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/notify")
public class NotificationController {

    @Resource
    private UserService userService;
    @Resource
    private NotificationService notificationService;

    @GetMapping("/list")
    public BaseResponse<Page<NotificationVO>> list(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<NotificationVO> page = notificationService.listMyNotifications(current, size, loginUser);
        return ResultUtils.success(page);
    }

    @PostMapping("/read/{id}")
    public BaseResponse<Boolean> markRead(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        notificationService.markRead(id, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/read/all")
    public BaseResponse<Boolean> markAllRead(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        notificationService.markAllRead(loginUser);
        return ResultUtils.success(true);
    }
}