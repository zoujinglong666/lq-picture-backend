package com.zjl.lqpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户发送验证码请求
 *
 * @author zou
 */
@Data
public class UserSendCodeRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120794L;

    /**
     * 邮箱地址
     */
    private String email;
}