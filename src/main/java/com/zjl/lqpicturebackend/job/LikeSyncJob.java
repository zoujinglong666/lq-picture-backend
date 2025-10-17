package com.zjl.lqpicturebackend.job;

import com.zjl.lqpicturebackend.mapper.PictureLikeMapper;
import com.zjl.lqpicturebackend.model.PictureLike;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.Date;

/**
 * 每分钟将 Redis 的用户点赞状态同步到 MySQL
 * 使用 Redis Key：
 * - pic:delta:keys       记录哪些图片有用户点赞变动
 * - pic:pending:{picId}  存储该图片有哪些 userId 待处理
 * - pic:liked:{uid}:{pid} 是否存在表示点赞
 */
@Slf4j
@Component
public class LikeSyncJob {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PictureLikeMapper pictureLikeMapper;

    /**
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void syncPendingLikes() {
        log.info("⭐ [LikeSyncJob] 开始执行 Redis → MySQL 点赞同步任务");

        String deltaSetKey = "pic:delta:keys";
        Set<String> picIds = stringRedisTemplate.opsForSet().members(deltaSetKey);

        if (picIds == null || picIds.isEmpty()) {
            log.info("🟡 [LikeSyncJob] 无需同步，deltaSetKey 中没有图片ID");
            return;
        }

        // 遍历所有有更新的图片ID
        for (String pidStr : picIds) {
            Long picId = Long.valueOf(pidStr);
            String pendingSet = "pic:pending:" + picId;

            Set<String> userIds = stringRedisTemplate.opsForSet().members(pendingSet);
            log.info("🟢 [LikeSyncJob] 图片ID={}，待同步用户数量={}", picId,
                    (userIds == null ? 0 : userIds.size()));

            if (userIds == null || userIds.isEmpty()) {
                // 无用户待处理，移除该图片ID索引
                stringRedisTemplate.opsForSet().remove(deltaSetKey, pidStr);
                log.info("⚪ [LikeSyncJob] 图片ID={} 无用户待处理，已清除 delta 标记", picId);
                continue;
            }

            // 遍历该图片下所有用户的状态变更
            for (String uidStr : userIds) {
                Long userId = Long.valueOf(uidStr);
                String likedKey = "pic:liked:" + userId + ":" + picId;

                Boolean likedExists = stringRedisTemplate.hasKey(likedKey);

                if (Boolean.TRUE.equals(likedExists)) {
                    // 点赞或恢复点赞：优先查是否存在
                    PictureLike exist = pictureLikeMapper.selectOneIncludeDeleted(picId, userId);
                    if (exist == null) {
                        // 数据库无记录 -> 插入新记录（MP 生成 ID）
                        PictureLike newLike = new PictureLike();
                        newLike.setPictureId(picId);
                        newLike.setUserId(userId);
                        newLike.setCreateTime(new Date());
                        newLike.setIsDelete(0);
                        pictureLikeMapper.insert(newLike);
                        log.info("👍 [LikeSyncJob] 用户 {} 对图片 {} 点赞 → insert", userId, picId);
                    } else if (exist.getIsDelete() != null && exist.getIsDelete() != 0) {
                        // 记录存在但被逻辑删除 -> 恢复
                        pictureLikeMapper.restoreLike(exist.getId());
                        log.info("👍 [LikeSyncJob] 用户 {} 对图片 {} 点赞 → restore", userId, picId);
                    } else {
                        // 已是点赞态，无需动作
                        log.info("👍 [LikeSyncJob] 用户 {} 对图片 {} 点赞 → already ok", userId, picId);
                    }
                } else {
                    // 取消点赞：逻辑删除（幂等）
                    pictureLikeMapper.cancelLike(picId, userId);
                    log.info("💔 [LikeSyncJob] 用户 {} 对图片 {} 取消点赞 → cancel", userId, picId);
                }

                // 从 Redis pending 集合中移除该用户
                stringRedisTemplate.opsForSet().remove(pendingSet, uidStr);
            }

            // 所有用户处理完，移除 delta 标记
            stringRedisTemplate.opsForSet().remove(deltaSetKey, pidStr);
            log.info("✅ [LikeSyncJob] 图片 {} 全部同步完成，已移除 delta 标记", picId);
        }

        log.info("🎯 [LikeSyncJob] 本轮同步任务结束");
    }
}
