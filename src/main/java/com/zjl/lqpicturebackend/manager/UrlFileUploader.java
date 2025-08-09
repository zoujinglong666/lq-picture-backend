package com.zjl.lqpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class UrlFileUploader extends AbstractFileUploader {


    @Override
    protected void validateFile(Object source) {
        String fileUrl = (String) source;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 校验URL有效性
        // 1. 校验协议
        boolean isHttp = HttpUtil.isHttp(fileUrl);
        boolean isHttps = HttpUtil.isHttps(fileUrl);
        ThrowUtils.throwIf(!isHttp && !isHttps, ErrorCode.PARAMS_ERROR, "URL协议错误");
        try (HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute()) {
            if (httpResponse.getStatus() != 200) {
                //这里不建议报错
                return;
            }
            String contentType = httpResponse.header("Content-Type");
            String contentLength = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentType)) {
                final List<String> ALLOW_CONTENT_TYPE_LIST = Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPE_LIST.contains(contentType), ErrorCode.PARAMS_ERROR, "图片格式错误");
            }
            if (StrUtil.isNotBlank(contentLength)) {
                long length = NumberUtil.parseLong(contentLength);
                final long ONE_M = 1024 * 1024;
                ThrowUtils.throwIf(length > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
            }

        } catch (Exception e) {
            log.error("校验URL有效性失败", e);
        }

    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        // 下载文件到临时目录
        HttpUtil.downloadFile(fileUrl, file);
    }
}
