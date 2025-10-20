package com.zjl.lqpicturebackend.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.RandomUtil;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import cn.dev33.satoken.stp.StpUtil;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import com.zjl.lqpicturebackend.manager.MultipartFileUploader;
import com.zjl.lqpicturebackend.model.dto.file.UploadPictureResult;

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
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private JavaMailSender javaMailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Resource
    private MultipartFileUploader multipartFileUploader;



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
        userEntity.setUserName("user_" + RandomUtil.randomString(8));
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
            System.out.println("已经获取用户信息");
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
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "ascend".equals(sortOrder), sortField);
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

    @Override
    public boolean sendEmailCode(String email) {
        // TODO: 实现邮箱验证码发送功能
        // 1. 验证邮箱格式
        if (StrUtil.isBlank(email) || !email.contains("@")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        
        try {
            // 2. 生成6位随机验证码
            String code = RandomUtil.randomNumbers(6);
            
            // 3. 将验证码存储到Redis，设置5分钟过期
            String redisKey = "email_code:" + email;
            stringRedisTemplate.opsForValue().set(redisKey, code, 300, java.util.concurrent.TimeUnit.SECONDS);
            
            // 4. 发送邮件
            log.info("发送邮箱验证码到: {}, 验证码: {}", email, code);
            sendEmailWithCode(email, code);
            
            return true;
        } catch (Exception e) {
            log.error("发送邮箱验证码失败", e);
            return false;
        }
    }

    @Override
    public boolean verifyEmailCode(String email, String code) {
        // TODO: 实现邮箱验证码验证功能
        if (StrUtil.hasBlank(email, code)) {
            return false;
        }
        
        try {
            // 从Redis获取验证码
            String redisKey = "email_code:" + email;
            String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
            
            if (StrUtil.isBlank(storedCode)) {
                log.warn("验证码已过期或不存在: {}", email);
                return false;
            }
            
            // 验证码匹配
            boolean isValid = code.equals(storedCode);
            if (isValid) {
                // 验证成功后删除验证码
                stringRedisTemplate.delete(redisKey);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("验证邮箱验证码失败", e);
            return false;
        }
    }

    @Override
    public LoginUserVO emailLogin(String email, String code, HttpServletRequest request) {
        // TODO: 实现邮箱登录功能
        if (StrUtil.hasBlank(email, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱或验证码不能为空");
        }
        
        // 1. 验证邮箱验证码
        if (!verifyEmailCode(email, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }
        
        // 2. 查找用户（这里假设User表有email字段，如果没有需要先添加）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", email); // 暂时使用userAccount字段存储邮箱
        User user = this.baseMapper.selectOne(queryWrapper);
        
        // 3. 如果用户不存在，自动注册
        if (user == null) {
            user = new User();
            user.setUserAccount(email);
            user.setUserName("邮箱用户_" + email.substring(0, email.indexOf("@")));
            user.setUserRole(UserRoleEnum.USER.getValue());
            // 邮箱登录不需要密码，设置一个随机密码
            user.setUserPassword(getEncryptPassword(RandomUtil.randomString(16)));
            
            boolean save = this.save(user);
            if (!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
            }
        }
        
        // 4. 使用Sa-Token登录
        StpUtil.login(user.getId());
        StpUtil.getTokenSession().set(USER_LOGIN_STATE, user);
        
        return this.getLoginUserVO(user);
    }

    @Override
    public String uploadUserAvatar(MultipartFile file, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        String uploadPathPrefix = String.format("/avatar/%s/", loginUser.getId());
        UploadPictureResult uploadResult = multipartFileUploader.upload(file, uploadPathPrefix);

        User user = this.getById(loginUser.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        user.setUserAvatar(uploadResult.getUrl());
        boolean updated = this.updateById(user);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新头像失败");
        }
        // 同步登录态中的用户信息
        StpUtil.getTokenSession().set(USER_LOGIN_STATE, user);
        return uploadResult.getUrl();
    }
    
    /**
     * 发送邮箱验证码
     */
    private void sendEmailWithCode(String email, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("龙琪图库登录验证码");
            helper.setText(generateEmailTemplate(code), true);
            
            javaMailSender.send(message);
            log.info("邮件发送成功: {}", email);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", email, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败");
        }
    }
    
    /**
     * 生成邮箱验证码HTML模板
     */
    private String generateEmailTemplate(String code) {
        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>" +
                        "</head>" +
                        "<body style='margin:0; padding:0; background-color:#f8f9fa; font-family: Arial, sans-serif;'>" +

                        // 外层容器
                        "<div style='max-width: 500px; width: 90%%; margin: 0 auto; background: #f8f9fa; border-radius: 8px; border: 1px solid #eaeaea; overflow: hidden;'>" +

                        // 顶部渐变横幅 + Logo
                        "<div style='background: linear-gradient(90deg, #007bff, #00c6ff); padding: 15px; text-align: center;'>" +
                        "<img src='https://example.com/logo.png' alt='图库 Logo' style='width: 40px; height: 40px; border-radius: 50%%; display: block; margin: 0 auto 8px;'>" +
                        "<h2 style='color: white; font-size: 20px; margin: 0;'>龙琪图库登录验证码</h2>" +
                        "<p style='color: rgba(255,255,255,0.8); font-size: 12px; margin: 4px 0 0;'>Security Verification Code</p>" +
                        "</div>" +

                        // 验证码卡片
                        "<div style='padding: 15px; background: white; border-radius: 0 0 8px 8px; text-align: center;'>" +
                        "<p style='font-size: 14px; color: #555; margin-bottom: 8px;'>您的验证码是：</p>" +
                        "<div style='font-size: 26px; font-weight: bold; color: #007bff; letter-spacing: 2px; padding: 8px 0;'>%s</div>" +
                        "<p style='color: #666; font-size: 12px; margin-top: 8px;'>验证码 5 分钟内有效，请及时使用。</p>" +
                        "</div>" +

                        // 底部提示
                        "<p style='color: #999; font-size: 11px; text-align: center; margin: 15px;'>如果这不是您的操作，请忽略此邮件。</p>" +
                        "</div>" +

                        "</body>" +
                        "</html>", code
        );
    }

}






