package com.zjl.lqpicturebackend.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.exception.ThrowUtils;
import com.zjl.lqpicturebackend.mapper.SpaceMapper;
import com.zjl.lqpicturebackend.model.Space;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.dto.space.SpaceAddRequest;
import com.zjl.lqpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zjl.lqpicturebackend.model.enums.SpaceLevelEnum;
import com.zjl.lqpicturebackend.model.vo.SpaceVO;
import com.zjl.lqpicturebackend.model.vo.UserVO;
import com.zjl.lqpicturebackend.service.SpaceService;
import com.zjl.lqpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zou
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-07-31 21:11:36
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private SpaceMapper spaceMapper;

    @Resource
    private UserService userService;


    @Resource
    private TransactionTemplate transactionTemplate;


    @Override
    public Long createSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户（安全性第一步）
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未登录");
        }

        Long userId = loginUser.getId();

        // 2. 构建 Space 实体，并拷贝请求参数
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);

        // 3. 设置默认空间名
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }

        // 4. 设置默认空间等级
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

        // 5. 设置默认值（根据空间等级填充空间规则等）
        this.fillSpaceBySpaceLevel(space);

        // 6. 参数校验（自定义逻辑，判断名称长度、合法性等）
        this.validSpace(space);

        // 7. 设置用户ID
        space.setUserId(userId);

        // 8. 权限校验（非管理员不能创建高级空间）
        if (space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue()
                && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }

        // 9. 控制并发：一个用户只能有一个“默认空间”
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
                // 10. 使用 Spring 的编程式事务来保证原子性和一致性
                Long spaceId = transactionTemplate.execute(status -> {
                    // 10.1 查询是否已存在默认空间
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .eq(Space::getSpaceLevel, SpaceLevelEnum.COMMON.getValue())
                            .exists();
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "该用户已存在默认空间");

                    // 10.2 执行保存
                    boolean saveResult = this.save(space);
                    if (!saveResult) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存空间失败");
                    }

                    return space.getId(); // 10.3 返回新空间ID
                });
                return spaceId;
        }
    }


    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void validSpace(Space space) {
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);

        boolean add = space.getId() == null;
        if (add) {
            if (spaceName == null || spaceName.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不存在");
            }
        }
        if (spaceName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能超过20个字符");
        }



    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }




//    public QueryWrapper<Space> getQueryWrapper1(SpaceQueryRequest request) {
//        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
//        Field[] fields = SpaceQueryRequest.class.getDeclaredFields();
//        for (Field field : fields) {
//            field.setAccessible(true);
//            try {
//                Object value = field.get(request);
//                if (ObjUtil.isNotEmpty(value)) {
//                    // 属性名转为列名，例如 userId -> user_id（根据需要）
//                    TableField tableField = field.getAnnotation(TableField.class);
//                    String column = (tableField != null && !tableField.value().isEmpty())
//                            ? tableField.value()
//                            : field.getName();
//                    queryWrapper.eq(column, value);
//                }
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            }
//        }
//        return queryWrapper;
//    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }


}



