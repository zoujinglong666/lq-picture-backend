package com.zjl.lqpicturebackend.model.dto.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.zjl.lqpicturebackend.common.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 查询评论请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor  // ✅ 无参构造，Jackson 需要
@AllArgsConstructor // 可选，全参构造，方便手动 new
public class CommentQueryRequest extends PageRequest implements Serializable {

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

    private static final long serialVersionUID = 1L;
}
