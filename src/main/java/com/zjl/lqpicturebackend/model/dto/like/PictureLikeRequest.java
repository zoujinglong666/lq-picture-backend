package com.zjl.lqpicturebackend.model.dto.like;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zou
 */
@Data
public class PictureLikeRequest implements Serializable {
    private Long pictureId;
    private static final long serialVersionUID = 1L;

}