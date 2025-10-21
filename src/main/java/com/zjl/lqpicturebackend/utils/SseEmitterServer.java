package com.zjl.lqpicturebackend.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjl.lqpicturebackend.model.vo.NotificationVO;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterServer {

    // 使用 ConcurrentHashMap 存储 SseEmitter，Key 为 userId
    private static final Map<Long, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 SSE 连接
     *
     * @param userId 用户ID
     * @return SseEmitter
     */
    public SseEmitter createSse(Long userId) {
        // 设置超时时间为0，表示永不超时，但我们会在 onCompletion 和 onTimeout 中处理
        SseEmitter sseEmitter = new SseEmitter(0L);
        // 连接完成时，从 Map 中移除
        sseEmitter.onCompletion(() -> removeSse(userId));
        // 连接超时时，从 Map 中移除
        sseEmitter.onTimeout(() -> removeSse(userId));
        // 连接发生错误时，从 Map 中移除
        sseEmitter.onError(throwable -> removeSse(userId));
        sseEmitterMap.put(userId, sseEmitter);
        return sseEmitter;
    }

    /**
     * 移除 SSE 连接
     *
     * @param userId 用户ID
     */
    public void removeSse(Long userId) {
        sseEmitterMap.remove(userId);
    }

    /**
     * 发送消息给指定用户
     *
     * @param userId         用户ID
     * @param notificationVO 通知内容
     */
    public void sendMessage(Long userId, NotificationVO notificationVO) {
        SseEmitter sseEmitter = sseEmitterMap.get(userId);
        if (sseEmitter != null) {
            try {
                // Spring MVC 会自动处理 JSON 序列化，但手动处理可以提供更好的错误控制
                String message = objectMapper.writeValueAsString(notificationVO);
                sseEmitter.send(SseEmitter.event().name("notification").data(message));
            } catch (IOException e) {
                // 发送失败，移除该连接
                removeSse(userId);
            }
        }
    }
}