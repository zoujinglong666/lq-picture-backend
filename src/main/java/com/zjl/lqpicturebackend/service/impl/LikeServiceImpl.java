package com.zjl.lqpicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.mapper.PictureLikeMapper;
import com.zjl.lqpicturebackend.model.PictureLike;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.service.LikeService;
import org.springframework.stereotype.Service;
import java.util.Date;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeServiceImpl extends ServiceImpl<PictureLikeMapper, PictureLike> implements LikeService {
    // Service 方法

    @Override
    @Transactional // 保证点赞/取消操作与数据库状态一致
    public PictureLikeResponse toggleLike(Long pictureId, User loginUser) {
        if (loginUser == null) {
            // 用户未登录，抛出异常
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }

        Long userId = loginUser.getId();

        // =========================
        // 1. 查询当前用户点赞状态（一次查询）
        // =========================
        PictureLike exist = this.baseMapper.selectOneIncludeDeleted(pictureId, userId); // 查询包含已删除记录

        boolean liked; // 用于返回给前端当前用户是否点赞

        // =========================
        // 2. 点赞 / 取消点赞 / 恢复点赞
        // =========================
        if (exist != null && exist.getIsDelete() == 0) {
            // 用户已点赞 -> 执行取消点赞（逻辑删除）
            this.remove(new LambdaQueryWrapper<PictureLike>()
                    .eq(PictureLike::getPictureId, pictureId)
                    .eq(PictureLike::getUserId, userId)); // 逻辑删除：置 is_delete = 1
            liked = false; // 当前用户已取消点赞
        } else if (exist != null) {
            // 用户之前取消过 -> 恢复点赞
            this.baseMapper.restoreLike(exist.getId()); // 恢复点赞：绕过逻辑删除拦截
            liked = true; // 当前用户已点赞
        } else {
            // 用户从未点赞过 -> 新增点赞记录
            PictureLike like = new PictureLike();
            like.setPictureId(pictureId);
            like.setUserId(userId);
            like.setCreateTime(new Date());
            like.setIsDelete(0); // 标记为有效点赞
            this.save(like);
            liked = true; // 当前用户已点赞
        }

        // =========================
        // 3. 查询当前图片总点赞数（一次查询）
        // =========================
        int likeCount = (int) this.count(new LambdaQueryWrapper<PictureLike>()
                .eq(PictureLike::getPictureId, pictureId) // 统计当前图片
                .eq(PictureLike::getIsDelete, 0));       // 只统计未删除的点赞

        // =========================
        // 4. 返回结果给前端
        // =========================
        return new PictureLikeResponse(likeCount, liked);
    }




    @Override
    public long countByPictureId(Long pictureId) {
        return this.lambdaQuery().eq(PictureLike::getPictureId, pictureId).eq(PictureLike::getIsDelete, 0).count();
    }

    @Override
    public boolean hasLiked(Long pictureId, Long userId) {
        if (userId == null) return false;
        return this.lambdaQuery().eq(PictureLike::getPictureId, pictureId)
                .eq(PictureLike::getUserId, userId)
                .eq(PictureLike::getIsDelete, 0).count() > 0;
    }
}