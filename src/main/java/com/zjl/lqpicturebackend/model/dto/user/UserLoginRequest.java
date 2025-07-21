package com.zjl.lqpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: ZJL
 * Date: 2022/6/5
 * @author zou
 */
@Data
public class UserLoginRequest implements Serializable {

	private String userAccount;
	private String userPassword;
}
