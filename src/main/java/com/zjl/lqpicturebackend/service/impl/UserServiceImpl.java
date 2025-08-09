package com.zjl.lqpicturebackend.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.dto.user.UserQueryRequest;
import com.zjl.lqpicturebackend.model.dto.user.UserRegisterRequest;
import com.zjl.lqpicturebackend.model.enums.UserRoleEnum;
import com.zjl.lqpicturebackend.model.vo.LoginUserVO;
import com.zjl.lqpicturebackend.model.vo.UserVO;
import com.zjl.lqpicturebackend.service.UserService;
import com.zjl.lqpicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
  import cn.dev33.satoken.stp.StpUtil;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zjl.lqpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author zou
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    public static final String PASSWORD_SALT = "LQ_Picture_Backend_Salt_1124";



    @Override
    public Integer userLogout(HttpServletRequest request) {
        try {
            // 使用Sa-Token进行登出
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
            // 兼容性处理：同时清除传统Session
            request.getSession().removeAttribute(USER_LOGIN_STATE);
            return 1;
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return 0;
        }
    }



    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();


        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        if (userAccount.length() < 4 || userAccount.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度必须在6-20之间");
        }
        if (userPassword.length() < 8 || userPassword.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须在8-20之间");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码输入不一致");
        }

        //2.检查用户名是否存在

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }


        //3.密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        //4.保存用户信息
        User userEntity = new User();
        userEntity.setUserAccount(userAccount);
        userEntity.setUserPassword(encryptPassword);
        userEntity.setUserName("无名");
        userEntity.setUserRole(UserRoleEnum.USER.getValue());

        boolean save = this.save(userEntity);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存用户信息失败");
        }

        //3.返回用户id
        return userEntity.getId();
    }



    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        if (userAccount.length() < 4 || userAccount.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度错误");
        }
        if (userPassword.length() < 8 || userPassword.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度错误");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.baseMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不存在");
        }

        String encryptPassword = getEncryptPassword(userPassword);
        if (!encryptPassword.equals(user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 使用 Sa-Token 登录
        StpUtil.login(user.getId()); // 使用用户 ID 登录

        // 将用户信息存储到 Sa-Token 的 Session 中
        StpUtil.getTokenSession().set(USER_LOGIN_STATE, user);

        return this.getLoginUserVO(user);
    }



    /**
     * @param password 密码明文
     * @return static 不能使用@Override注解，否则会报错：
     */
    @Override
    public String getEncryptPassword(String password) {
        String encryptPassword = password + PASSWORD_SALT;
        return DigestUtils.md5DigestAsHex(encryptPassword.getBytes());
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        System.out.println("tokenInfo:" + tokenInfo);
        // 第3步，返回给前端
        // 生成 Token 并返回给客户端
        String token = StpUtil.getTokenValue();
        // 将 Token 添加到返回的用户信息中
        loginUserVO.setToken(token);
        BeanUtil.copyProperties(user, loginUserVO);
        log.info("loginUserVO:{}", loginUserVO);
        return loginUserVO;

    }
    @Override
    public User getLoginUser(HttpServletRequest request) {
        try {
            // 检查是否已登录
            if (!StpUtil.isLogin()) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
            }
            
            // 从Session中获取用户信息
            User userObj = (User) StpUtil.getTokenSession().get(USER_LOGIN_STATE);
            if (userObj == null) {
                // 如果Session中没有用户信息，
                System.out.println("Session中没有用户信息,重新获取用户信息");
                Object loginId = StpUtil.getLoginId();
                if (loginId != null) {
                    userObj = this.getById((Long) loginId);
                    if (userObj != null) {
                        // 重新存储到Session中
                        StpUtil.getTokenSession().set(USER_LOGIN_STATE, userObj);
                    }
                }
            }
            
            if (userObj == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "获取用户信息失败");
            }
            
            return userObj;
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            log.error("获取登录用户信息失败", e);
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或token无效");
        }
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAdmin(User user) {
        if(user == null){
            return false;
        }
        return user.getUserRole().equals(UserRoleEnum.ADMIN.getValue());
    }
}




