package com.zjl.lqpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zou
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 5689053578644195575L;
    private String userAccount;
    private String userPassword;
    private String checkPassword;


}
