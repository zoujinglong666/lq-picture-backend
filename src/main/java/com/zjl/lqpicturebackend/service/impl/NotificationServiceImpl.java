package com.zjl.lqpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.mapper.NotificationMapper;
import com.zjl.lqpicturebackend.model.Notification;
import com.zjl.lqpicturebackend.model.Picture;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.vo.NotificationVO;
import com.zjl.lqpicturebackend.service.NotificationService;
import com.zjl.lqpicturebackend.service.PictureService;
import com.zjl.lqpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;

    @Override
    public void createForLike(Long pictureId, Long actorUserId) {
        Picture p = pictureService.getById(pictureId);
        if (p == null) return;
        if (p.getUserId().equals(actorUserId)) return; // 不给自己发
        Notification n = new Notification();
        n.setUserId(p.getUserId());
        n.setType("LIKE");
        n.setRefId(null);
        n.setPictureId(pictureId);
        n.setContent("你的图片收到一个点赞");
        n.setReadStatus(0);
        n.setCreateTime(new Date());
        n.setIsDelete(0);
        this.save(n);
    }

    @Override
    public void createForComment(Long pictureId, Long commentId, Long actorUserId) {
        Picture p = pictureService.getById(pictureId);
        if (p == null) return;
        if (p.getUserId().equals(actorUserId)) return;
        Notification n = new Notification();
        n.setUserId(p.getUserId());
        n.setType("COMMENT");
        n.setRefId(commentId);
        n.setPictureId(pictureId);
        n.setContent("你的图片收到一条评论");
        n.setReadStatus(0);
        n.setCreateTime(new Date());
        n.setIsDelete(0);
        this.save(n);
    }

    @Override
    public Page<NotificationVO> listMyNotifications(long current, long size, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }
        Page<Notification> page = this.lambdaQuery()
                .eq(Notification::getUserId, loginUser.getId())
                .eq(Notification::getIsDelete, 0)
                .orderByDesc(Notification::getCreateTime)
                .page(new Page<>(current, size));
        List<NotificationVO> list = page.getRecords().stream().map(NotificationVO::from).collect(Collectors.toList());
        Page<NotificationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(list);
        return voPage;
    }

    @Override
    public void markRead(Long id, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }
        Notification n = this.getById(id);
        if (n == null || n.getIsDelete() != 0 || !n.getUserId().equals(loginUser.getId())) {
            return;
        }
        n.setReadStatus(1);
        this.updateById(n);
    }

    @Override
    public void markAllRead(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }
        this.lambdaUpdate()
                .eq(Notification::getUserId, loginUser.getId())
                .eq(Notification::getIsDelete, 0)
                .set(Notification::getReadStatus, 1)
                .update();
    }
}