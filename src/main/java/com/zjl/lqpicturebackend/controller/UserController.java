package com.zjl.lqpicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjl.lqpicturebackend.annotation.AuthCheck;
import com.zjl.lqpicturebackend.common.BaseResponse;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.common.ResultUtils;
import com.zjl.lqpicturebackend.constant.UserConstant;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.exception.ThrowUtils;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.dto.user.UserLoginRequest;
import com.zjl.lqpicturebackend.model.dto.user.UserQueryRequest;
import com.zjl.lqpicturebackend.model.dto.user.UserRegisterRequest;
import com.zjl.lqpicturebackend.model.dto.user.UserUpdateRequest;
import com.zjl.lqpicturebackend.model.vo.LoginUserVO;
import com.zjl.lqpicturebackend.model.vo.UserVO;
import com.zjl.lqpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;


/**
 * @author zou
 */
@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;


    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {

        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {

        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录成功后，生成token，并返回给客户端
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);


        if (loginUserVO == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，请稍后再试");
        }
        return ResultUtils.success(loginUserVO);

    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        // 校验参数是否为空
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }


    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUser(UserQueryRequest userQueryRequest,HttpServletRequest request) {
        // 校验参数是否为空

        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);

    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    
    @PostMapping("/update/info")
    public BaseResponse<LoginUserVO> updateUserInfo(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if(userUpdateRequest==null||userUpdateRequest.getId()==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询原用户
        User user = userService.getById(userUpdateRequest.getId());
        if(user == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        // 只更新允许的字段
        if(userUpdateRequest.getUserAccount() != null){
            user.setUserAccount(userUpdateRequest.getUserAccount());
        }
        if(userUpdateRequest.getUserName() != null){
            user.setUserName(userUpdateRequest.getUserName());
        }
        if(userUpdateRequest.getUserAvatar() != null){
            user.setUserAvatar(userUpdateRequest.getUserAvatar());
        }
        if(userUpdateRequest.getUserProfile() != null){
            user.setUserProfile(userUpdateRequest.getUserProfile());
        }

        user.setEditTime(new Date());
        boolean result = userService.updateById(user);
        if(!result){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }
        // 返回最新登录用户信息
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

}
