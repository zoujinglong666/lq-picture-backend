package com.zjl.lqpicturebackend.model.vo;

import com.zjl.lqpicturebackend.model.Notification;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class NotificationVO implements Serializable {

    private Long id;
    private String type;
    private Long refId;
    private Long pictureId;
    private String content;
    private Integer readStatus;
    private Date createTime;

    // New fields for rich content
    private String pictureUrl;
    private Long actorId;
    private String actorName;
    private String actorAvatar;

    private static final long serialVersionUID = 1L;

    public static NotificationVO from(Notification n) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(n, vo);
        return vo;
    }
}