package com.zjl.lqpicturebackend.service;

import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.service.impl.PictureLikeResponse;

public interface LikeService {

    /**
     * 切换点赞（点赞/取消）
     * @return 切换后是否已点赞
     */
    PictureLikeResponse toggleLike(Long pictureId, User loginUser);

    long countByPictureId(Long pictureId);

    boolean hasLiked(Long pictureId, Long userId);
}