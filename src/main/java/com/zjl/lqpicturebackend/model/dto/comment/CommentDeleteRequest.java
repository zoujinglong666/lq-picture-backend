package com.zjl.lqpicturebackend.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommentDeleteRequest implements Serializable {
    private Long id;
    private static final long serialVersionUID = 1L;
}