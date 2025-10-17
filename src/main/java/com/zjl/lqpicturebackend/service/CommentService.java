package com.zjl.lqpicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.vo.CommentVO;

public interface CommentService {

    Long addComment(Long pictureId, String content, Long parentId, User loginUser);

    Page<CommentVO> listComments(Long pictureId, long current, long size);

    boolean deleteComment(Long commentId, User loginUser);

    long countByPictureId(Long pictureId);
}