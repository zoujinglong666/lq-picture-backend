package com.zjl.lqpicturebackend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 站内通知
 */
@TableName("notification")
@Data
public class Notification implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 接收者用户ID（图片作者）
     */
    private Long userId;

    /**
     * 通知类型：LIKE / COMMENT
     */
    private String type;

    /**
     * 关联ID：点赞ID或评论ID
     */
    private Long refId;

    /**
     * 关联图片ID
     */
    private Long pictureId;

    /**
     * 简要内容
     */
    private String content;

    /**
     * 0 未读，1 已读
     */
    private Integer readStatus;

    private Date createTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}