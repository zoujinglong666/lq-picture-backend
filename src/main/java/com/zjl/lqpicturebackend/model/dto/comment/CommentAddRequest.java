package com.zjl.lqpicturebackend.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommentAddRequest implements Serializable {
    private String content;
    private Long parentId;
    private static final long serialVersionUID = 1L;
}