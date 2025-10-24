package com.zjl.lqpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjl.lqpicturebackend.common.*;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.vo.NotificationVO;
import com.zjl.lqpicturebackend.service.NotificationService;
import com.zjl.lqpicturebackend.service.UserService;
import com.zjl.lqpicturebackend.utils.SseEmitterServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/notify")
public class NotificationController {

    @Resource
    private UserService userService;
    @Resource
    private NotificationService notificationService;
    @Resource
    private SseEmitterServer sseEmitterServer;


    public void SseController(UserService userService, SseEmitterServer sseEmitterServer) {
        this.userService = userService;
        this.sseEmitterServer = sseEmitterServer;
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "缺少或无效的Authorization头");
        }
        String token = authHeader.substring(7);

        User loginUser = userService.getLoginUserByToken(token);

        // 添加日志记录
        log.info("用户 {} 订阅通知成功", loginUser.getId());

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
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        notificationService.markRead(id, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/read/all")
    public BaseResponse<Boolean> markAllReadNotify(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        notificationService.markAllRead(loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/count/unread")
    public BaseResponse<Long> countUnread(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        long count = notificationService.countUnread(loginUser);
        return ResultUtils.success(count);
    }
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteNotify(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean res = notificationService.deleteNotification(deleteRequest.getId(), loginUser);
        return ResultUtils.success(res);
    }

    @PostMapping("/unsubscribe")
    public BaseResponse<Boolean> unsubscribe(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                User loginUser = userService.getLoginUserByToken(token);
                if (loginUser != null) {
                    sseEmitterServer.closeSse(loginUser.getId());
                    log.info("用户 {} 主动退订SSE通知", loginUser.getId());
                }
            } catch (BusinessException e) {
                // 如果token无效或用户找不到，忽略异常，因为这意味着连接可能已经关闭
                log.warn("退订SSE通知时token无效: {}", token);
            }
        }
        return ResultUtils.success(true);
    }
}