package com.zjl.lqpicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.mapper.PictureLikeMapper;
import com.zjl.lqpicturebackend.model.PictureLike;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.service.LikeService;
import org.springframework.stereotype.Service;
import java.util.Date;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class LikeServiceImpl extends ServiceImpl<PictureLikeMapper, PictureLike> implements LikeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Lua脚本：原子切换点赞状态 + 计数 + 记录待同步用户（从资源文件加载）
    private final DefaultRedisScript<List> toggleScript = new DefaultRedisScript<List>() {{
        setResultType(List.class);
        setScriptText(loadLua("lua/toggle_like.lua"));
    }};

    @Override
    @Transactional // 保证点赞/取消操作与数据库状态一致
    public PictureLikeResponse toggleLike(Long pictureId, User loginUser) {
        if (loginUser == null) {
            // 用户未登录，抛出异常
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }

        Long userId = loginUser.getId();

        // Redis 键
        String likedKey = "pic:liked:" + userId + ":" + pictureId;
        String countKey = "pic:count:" + pictureId;
        String pendingSet = "pic:pending:" + pictureId;
        String deltaSetKey = "pic:delta:keys";

        // 原子执行：切换点赞状态 + 更新计数 + 记录待同步用户与图片
        List<Object> res = stringRedisTemplate.execute(
                toggleScript,
                Arrays.asList(likedKey, countKey, pendingSet, deltaSetKey),
                String.valueOf(userId),
                String.valueOf(pictureId)
        );

        boolean liked = Integer.parseInt(String.valueOf(res.get(0))) == 1;
        int likeCount = Integer.parseInt(String.valueOf(res.get(1)));

        // 写穿 MySQL：当前用户本次操作立即落库，避免“慢半拍”
        if (liked) {
            PictureLike exist = this.baseMapper.selectOneIncludeDeleted(pictureId, userId);
            if (exist == null) {
                PictureLike newLike = new PictureLike();
                newLike.setPictureId(pictureId);
                newLike.setUserId(userId);
                newLike.setCreateTime(new Date());
                newLike.setIsDelete(0);
                this.baseMapper.insert(newLike); // MP 生成 ID
            } else if (exist.getIsDelete() != null && exist.getIsDelete() != 0) {
                this.baseMapper.restoreLike(exist.getId());
            }
        } else {
            this.baseMapper.cancelLike(pictureId, userId);
        }

        // 返回实时结果
        return new PictureLikeResponse(likeCount, liked);
    }

    // 读取 classpath 下的 Lua 脚本文本
    private String loadLua(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Load lua script failed: " + path, e);
        }
    }

    @Override
    public long countByPictureId(Long pictureId) {
        String countKey = "pic:count:" + pictureId;
        String val = stringRedisTemplate.opsForValue().get(countKey);
        if (val == null) return 0L;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public boolean hasLiked(Long pictureId, Long userId) {
        if (userId == null) return false;
        String likedKey = "pic:liked:" + userId + ":" + pictureId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(likedKey));
    }
}