package com.zjl.lqpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建扩图任务请求
 */
@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;


    private static final long serialVersionUID = 1L;
}