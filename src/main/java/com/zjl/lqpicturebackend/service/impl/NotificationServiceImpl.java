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
import com.zjl.lqpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zjl.lqpicturebackend.service.UserService;
import com.zjl.lqpicturebackend.utils.SseEmitterServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Lazy
    @Autowired
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private SseEmitterServer sseEmitterServer;
    @Resource
    private com.zjl.lqpicturebackend.mapper.PictureCommentMapper pictureCommentMapper;

    @Override
    public void createForLike(Long pictureId, Long actorUserId) {
        Picture p = pictureService.getById(pictureId);
        if (p == null) return;
        if (p.getUserId().equals(actorUserId)) return; // 不给自己发
        User actor = userService.getById(actorUserId);
        if (actor == null) {
            return;
        }
        Notification n = new Notification();
        n.setUserId(p.getUserId());
        n.setType("LIKE");
        n.setRefId(actorUserId);
        n.setPictureId(pictureId);
        n.setContent(String.format("用户 @%s 点赞了你的图片《%s》", actor.getUserName(), p.getName()));
        n.setReadStatus(0);
        n.setCreateTime(new Date());
        n.setIsDelete(0);
        this.save(n);
        // 推送实时通知
        sseEmitterServer.sendMessage(n.getUserId(), NotificationVO.from(n));
    }

    @Override
    public void createForComment(Long pictureId, Long commentId, Long actorUserId) {
        Picture p = pictureService.getById(pictureId);
        if (p == null) return;
        if (p.getUserId().equals(actorUserId)) return;
        User actor = userService.getById(actorUserId);
        if (actor == null) {
            return;
        }
        Notification n = new Notification();
        n.setUserId(p.getUserId());
        n.setType("COMMENT");
        n.setRefId(commentId);
        n.setPictureId(pictureId);
        // 读取评论内容用于展示
        String commentText = null;
        if (commentId != null) {
            com.zjl.lqpicturebackend.model.PictureComment c = pictureCommentMapper.selectById(commentId);
            if (c != null && c.getIsDelete() != null && c.getIsDelete() == 0) {
                commentText = c.getContent();
            }
        }
        if (commentText == null) {
            n.setContent(String.format("用户 @%s 评论了你的图片《%s》", actor.getUserName(), p.getName()));
        } else {
            n.setContent(String.format("用户 @%s 评论了你的图片《%s》\\n评论内容：%s", actor.getUserName(), p.getName(), commentText));
        }
        n.setReadStatus(0);
        n.setCreateTime(new Date());
        n.setIsDelete(0);
        this.save(n);
        // 推送实时通知
        sseEmitterServer.sendMessage(n.getUserId(), NotificationVO.from(n));
    }

    @Override
    public void createForPictureReview(Long pictureId, Integer reviewStatus) {
        Picture p = pictureService.getById(pictureId);
        if (p == null) return;

        Notification n = new Notification();
        n.setUserId(p.getUserId());
        n.setType("PICTURE_REVIEW");
        n.setPictureId(pictureId);
        if (reviewStatus.equals(PictureReviewStatusEnum.PASS.getValue())) {
            n.setContent(String.format("您上传的图片《%s》已通过审核", p.getName()));
        } else {
            n.setContent(String.format("您上传的图片《%s》未通过审核", p.getName()));
        }
        n.setReadStatus(0);
        n.setCreateTime(new Date());
        n.setIsDelete(0);
        this.save(n);
        // 推送实时通知
        sseEmitterServer.sendMessage(n.getUserId(), NotificationVO.from(n));
    }

    @Override
    public Page<NotificationVO> listMyNotifications(long current, long size, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }


        Page<Notification> page = this.lambdaQuery()
                .eq(Notification::getUserId, loginUser.getId())
                .eq(Notification::getIsDelete, 0)
                .orderByAsc(Notification::getReadStatus)
                .orderByDesc(Notification::getCreateTime)
                .page(new Page<>(current, size));

//        Page<Notification> page = this.lambdaQuery()
//                .eq(Notification::getUserId, loginUser.getId())
//                .eq(Notification::getIsDelete, 0)
//                .orderByDesc(Notification::getCreateTime)
//                .page(new Page<>(current, size));

        Page<NotificationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<Notification> records = page.getRecords();
        if (records.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        // 1. 提取所有相关的ID以便批量查询
        Set<Long> pictureIds = records.stream()
                .map(Notification::getPictureId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 假设 "LIKE" 和 "FOLLOW" 类型的 refId 是 actorUserId
        Set<Long> actorIds = records.stream()
                .filter(n -> "LIKE".equals(n.getType()) || "FOLLOW".equals(n.getType()))
                .map(Notification::getRefId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 批量获取图片和用户信息
        Map<Long, Picture> pictureMap = new HashMap<>();
        if (!pictureIds.isEmpty()) {
            pictureMap = pictureService.listByIds(pictureIds).stream()
                    .collect(Collectors.toMap(Picture::getId, Function.identity()));
        }

        Map<Long, User> userMap = new HashMap<>();
        if (!actorIds.isEmpty()) {
            userMap = userService.listByIds(actorIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
        }

        // 3. 组装 NotificationVO 列表
        Map<Long, Picture> finalPictureMap = pictureMap;
        Map<Long, User> finalUserMap = userMap;
        List<NotificationVO> voList = records.stream().map(n -> {
            NotificationVO vo = NotificationVO.from(n);

            // 填充图片信息
            if (n.getPictureId() != null) {
                Picture p = finalPictureMap.get(n.getPictureId());
                if (p != null) {
                    vo.setPictureUrl(p.getUrl());
                }
            }

            // 填充操作者信息 (点赞、关注等)
            if (("LIKE".equals(n.getType()) || "FOLLOW".equals(n.getType())) && n.getRefId() != null) {
                User actor = finalUserMap.get(n.getRefId());
                if (actor != null) {
                    vo.setActorId(actor.getId());
                    vo.setActorName(actor.getUserName());
                    vo.setActorAvatar(actor.getUserAvatar());
                }
            }
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
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

    @Override
    public long countUnread(User loginUser) {
        if (loginUser == null) {
            return 0;
        }
        return this.lambdaQuery()
                .eq(Notification::getUserId, loginUser.getId())
                .eq(Notification::getReadStatus, 0)
                .eq(Notification::getIsDelete, 0)
                .count();
    }

    @Override
    public boolean deleteNotification(Long id, User loginUser) {

        return this.lambdaUpdate()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, loginUser.getId())
                .set(Notification::getIsDelete, 1)
                .update();
    }
}