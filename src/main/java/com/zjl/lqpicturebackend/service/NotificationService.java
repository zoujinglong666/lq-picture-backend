package com.zjl.lqpicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.vo.NotificationVO;

public interface NotificationService {

    void createForLike(Long pictureId, Long actorUserId);

    void createForComment(Long pictureId, Long commentId, Long actorUserId);

    void createForPictureReview(Long pictureId, Integer reviewStatus);

    Page<NotificationVO> listMyNotifications(long current, long size, User loginUser);

    void markRead(Long id, User loginUser);

    void markAllRead(User loginUser);
}