package com.zjl.lqpicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.mapper.PictureCommentMapper;
import com.zjl.lqpicturebackend.model.PictureComment;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.vo.CommentVO;
import com.zjl.lqpicturebackend.service.CommentService;
import com.zjl.lqpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<PictureCommentMapper, PictureComment> implements CommentService {


    @Resource
    private UserService userService;

    @Override
    public Long addComment(Long pictureId, String content, Long parentId, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }
        if (content == null || content.trim().isEmpty() || content.length() > 1000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容不合法");
        }
        // 确认二级回复合法
        if (parentId != null) {
            PictureComment parent = this.getById(parentId);
            if (parent == null || parent.getIsDelete() != 0 || !parent.getPictureId().equals(pictureId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "父评论不合法");
            }
        }
        PictureComment comment = new PictureComment();
        comment.setPictureId(pictureId);
        comment.setUserId(loginUser.getId());
        comment.setParentId(parentId);
        comment.setContent(content.trim());
        comment.setCreateTime(new Date());
        comment.setIsDelete(0);
        this.save(comment);
        return comment.getId();
    }

    @Override
       /**
     * 分页查询图片的一级评论及其子回复
     *
     * @param pictureId 图片ID，用于筛选指定图片的评论
     * @param current 当前页码，从1开始
     * @param size 每页大小，即每页显示的记录数
     * @return 返回封装好的评论分页数据，包括一级评论和对应的子回复
     */
    public Page<CommentVO> listComments(Long pictureId, long current, long size) {
        // 查询未删除的一级评论（parentId为空），并按创建时间倒序排列
        Page<PictureComment> page = this.lambdaQuery()
                .eq(PictureComment::getPictureId, pictureId)
                .eq(PictureComment::getIsDelete, 0)
                .isNull(PictureComment::getParentId) // 一级评论
                .orderByDesc(PictureComment::getCreateTime)
                .page(new Page<>(current, size));

        List<PictureComment> parents = page.getRecords();
        if (parents.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 查询所有一级评论对应的子回复
        List<Long> parentIds = parents.stream().map(PictureComment::getId).collect(Collectors.toList());
        List<PictureComment> replies = this.lambdaQuery()
                .in(PictureComment::getParentId, parentIds)
                .eq(PictureComment::getIsDelete, 0)
                .orderByAsc(PictureComment::getCreateTime)
                .list();

        // 收集所有涉及的用户ID，并获取用户信息映射表
        List<Long> userIds = parents.stream().map(PictureComment::getUserId).collect(Collectors.toList());
        userIds.addAll(replies.stream().map(PictureComment::getUserId).collect(Collectors.toList()));
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 将子回复按照父评论ID进行分组
        Map<Long, List<PictureComment>> replyGroup = replies.stream().collect(Collectors.groupingBy(PictureComment::getParentId));

        // 构造返回的VO对象列表
        List<CommentVO> voList = parents.stream().map(pc -> CommentVO.from(pc, userMap, replyGroup.get(pc.getId()))).collect(Collectors.toList());

        // 构建最终的分页结果
        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }


    @Override
    public boolean deleteComment(Long commentId, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }
        PictureComment comment = this.getById(commentId);
        if (comment == null || comment.getIsDelete() != 0) {
            return true;
        }
        // 作者或管理员可删
        if (!loginUser.getId().equals(comment.getUserId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除评论");
        }
        comment.setIsDelete(1);
        return this.updateById(comment);
    }

    @Override
    public long countByPictureId(Long pictureId) {
        return this.lambdaQuery().eq(PictureComment::getPictureId, pictureId).eq(PictureComment::getIsDelete, 0).count();
    }
}