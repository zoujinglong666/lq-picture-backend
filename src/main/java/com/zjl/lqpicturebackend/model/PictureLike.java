package com.zjl.lqpicturebackend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片点赞
 */
@TableName("picture_like")
@Data
public class PictureLike implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long pictureId;

    private Long userId;

    private Date createTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}