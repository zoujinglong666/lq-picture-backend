package com.zjl.lqpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户邮箱登录请求
 *
 * @author zou
 */
@Data
public class UserEmailLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 验证码
     */
    private String code;
}