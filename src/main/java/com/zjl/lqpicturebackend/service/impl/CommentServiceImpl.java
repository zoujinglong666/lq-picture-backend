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
import com.zjl.lqpicturebackend.model.vo.UserVO;
import com.zjl.lqpicturebackend.service.CommentService;
import com.zjl.lqpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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

       /**
     * 分页查询图片的评论（支持多级评论）
     *
     * @param pictureId 图片ID，用于筛选指定图片的评论
     * @param current 当前页码，从1开始
     * @param size 每页大小，即每页显示的记录数
     * @return 返回封装好的评论分页数据，包括所有层级的评论
     */
    @Override
    public Page<CommentVO> listComments(Long pictureId, long current, long size) {

        // Step 1️⃣ 查询一级评论（分页）
        Page<PictureComment> parentPage = this.lambdaQuery()
                .eq(PictureComment::getPictureId, pictureId)
                .eq(PictureComment::getIsDelete, 0)
                .isNull(PictureComment::getParentId)
                .orderByDesc(PictureComment::getCreateTime)
                .page(new Page<>(current, size));

        List<PictureComment> parents = parentPage.getRecords();
        if (parents.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // Step 2️⃣ 查询该图片下的所有评论（用于构建多级结构）
        List<PictureComment> allComments = this.lambdaQuery()
                .eq(PictureComment::getPictureId, pictureId)
                .eq(PictureComment::getIsDelete, 0)
                .orderByAsc(PictureComment::getCreateTime)
                .list();

        // Step 3️⃣ 收集所有涉及的用户ID
        Set<Long> userIds = allComments.stream()
                .map(PictureComment::getUserId)
                .collect(Collectors.toSet());

        // Step 4️⃣ 获取用户信息映射表
        Map<Long, User> userMap;
        if (!userIds.isEmpty()) {
            userMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        } else {
            userMap = new HashMap<>();
        }

        // Step 5️⃣ 构建评论树结构
        List<CommentVO> voList = buildCommentTree(parents, allComments, userMap);

        // Step 6️⃣ 构造返回分页对象
        Page<CommentVO> voPage = new Page<>(parentPage.getCurrent(), parentPage.getSize(), parentPage.getTotal());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 构建多级评论树结构
     */
    private List<CommentVO> buildCommentTree(List<PictureComment> parents, 
                                            List<PictureComment> allComments, 
                                            Map<Long, User> userMap) {
        // 按parentId分组所有评论（包括所有有父评论的评论）
        Map<Long, List<PictureComment>> commentMap = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(PictureComment::getParentId));

        // 调试日志：检查分组结果
        System.out.println("评论分组结果:");
        commentMap.forEach((parentId, comments) -> {
            System.out.println("父评论ID: " + parentId + ", 子评论数量: " + comments.size());
            comments.forEach(comment -> System.out.println("  - 子评论: " + comment));
        });

        return parents.stream()
                .map(parent -> buildCommentVO(parent, commentMap, userMap))
                .collect(Collectors.toList());
    }

    /**
     * 递归构建评论VO（支持无限层级）
     */
    private CommentVO buildCommentVO(PictureComment comment, 
                                    Map<Long, List<PictureComment>> commentMap,
                                    Map<Long, User> userMap) {
        CommentVO vo = new CommentVO();
        org.springframework.beans.BeanUtils.copyProperties(comment, vo);
        
        // 设置用户信息
        User u = userMap.get(comment.getUserId());
        if (u != null) {
            UserVO userVO = new UserVO();
            org.springframework.beans.BeanUtils.copyProperties(u, userVO);
            vo.setUser(userVO);
        }

        // 调试日志：检查当前评论
        System.out.println("构建评论VO: ID=" + comment.getId() + ", ParentID=" + comment.getParentId() + ", UserID=" + comment.getUserId());

        // 递归构建子评论
        List<PictureComment> children = commentMap.get(comment.getId());
        if (children != null && !children.isEmpty()) {
            System.out.println("评论 " + comment.getId() + " 有 " + children.size() + " 个子评论");
            List<CommentVO> childVOs = children.stream()
                    .map(child -> buildCommentVO(child, commentMap, userMap))
                    .collect(Collectors.toList());
            vo.setReplies(childVOs);
            System.out.println("评论 " + comment.getId() + " 构建完成，子评论数量: " + childVOs.size());
        } else {
            System.out.println("评论 " + comment.getId() + " 没有子评论");
        }

        return vo;
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