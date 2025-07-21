package com.zjl.lqpicturebackend.aop;

import com.zjl.lqpicturebackend.annotation.AuthCheck;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.enums.UserRoleEnum;
import com.zjl.lqpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author zou
 */

@Aspect
@Component
public class AuthInterceptor {

    //TODO: 实现权限拦截器
    @Resource
    private UserService userService;


    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //TODO: 实现权限拦截器逻辑，如果用户没有权限，则返回null，否则返回target

        String mustRole = authCheck.mustRole();


        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();


        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();


        UserRoleEnum mustRoleEnum = UserRoleEnum.fromValue(mustRole);
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }


        User loginUser = userService.getLoginUser(request);


        UserRoleEnum userRoleEnum = UserRoleEnum.fromValue(loginUser.getUserRole());

        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }


        if (!UserRoleEnum.ADMIN.equals(userRoleEnum) && UserRoleEnum.ADMIN.equals(mustRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();


    }


}
