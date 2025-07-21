package com.zjl.lqpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjl.lqpicturebackend.model.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjl.lqpicturebackend.model.dto.user.UserQueryRequest;
import com.zjl.lqpicturebackend.model.dto.user.UserRegisterRequest;
import com.zjl.lqpicturebackend.model.vo.LoginUserVO;
import com.zjl.lqpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author zou
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-06-12 21:51:36
 */
public interface UserService extends IService<User> {

    Integer userLogout(HttpServletRequest request);

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取加密后的密码 用于存储到数据库
     * @param password
     * @return
     */
    String getEncryptPassword(String password);

    /**
     * 根据用户信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    User getLoginUser(HttpServletRequest request);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> records);





    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

}
