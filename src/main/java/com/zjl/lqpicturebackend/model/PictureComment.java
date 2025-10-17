package com.zjl.lqpicturebackend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片评论（支持二级回复）
 */
@TableName("picture_comment")
@Data
public class PictureComment implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long pictureId;

    private Long userId;

    /**
     * 父评论ID，一级评论为 null
     */
    private Long parentId;

    /**
     * 评论内容（最长 1000）
     */
    private String content;

    private Date createTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}