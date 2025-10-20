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
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @return 是否发送成功
     */
    boolean sendEmailCode(String email);

    /**
     * 验证邮箱验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyEmailCode(String email, String code);

    /**
     * 邮箱登录
     * @param email 邮箱地址
     * @param code 验证码
     * @param request HTTP请求
     * @return 登录用户信息
     */
    LoginUserVO emailLogin(String email, String code, HttpServletRequest request);

    /**
     * 上传用户头像，返回头像URL
     * @param file 头像文件
     * @param loginUser 当前登录用户
     * @return 头像访问URL
     */
    String uploadUserAvatar(MultipartFile file, User loginUser);
}
