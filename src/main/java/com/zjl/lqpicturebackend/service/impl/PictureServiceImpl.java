package com.zjl.lqpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjl.lqpicturebackend.common.ErrorCode;
import com.zjl.lqpicturebackend.constant.UserConstant;
import com.zjl.lqpicturebackend.exception.BusinessException;
import com.zjl.lqpicturebackend.exception.ThrowUtils;
import com.zjl.lqpicturebackend.manager.AbstractFileUploader;
import com.zjl.lqpicturebackend.manager.FileManager;
import com.zjl.lqpicturebackend.manager.MultipartFileUploader;
import com.zjl.lqpicturebackend.manager.UrlFileUploader;
import com.zjl.lqpicturebackend.mapper.PictureMapper;
import com.zjl.lqpicturebackend.mapper.PictureLikeMapper;
import com.zjl.lqpicturebackend.mapper.PictureCommentMapper;
import com.zjl.lqpicturebackend.model.Picture;
import com.zjl.lqpicturebackend.model.PictureLike;
import com.zjl.lqpicturebackend.model.PictureComment;
import com.zjl.lqpicturebackend.model.Space;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.dto.file.UploadPictureResult;
import com.zjl.lqpicturebackend.model.dto.picture.*;
import com.zjl.lqpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zjl.lqpicturebackend.model.vo.PictureVO;
import com.zjl.lqpicturebackend.model.vo.UserVO;
import com.zjl.lqpicturebackend.service.PictureService;
import com.zjl.lqpicturebackend.service.SpaceService;
import com.zjl.lqpicturebackend.service.UserService;
import com.zjl.lqpicturebackend.service.LikeService;
import com.zjl.lqpicturebackend.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zou
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-07-06 16:09:09
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {


    @Resource
    FileManager fileManager;

    @Resource
    UserService userService;
    @Resource
    SpaceService spaceService;

    @Resource
    private MultipartFileUploader multipartFileUploader;

    @Resource
    private UrlFileUploader urlFileUploader;

    @Resource
    private PictureLikeMapper pictureLikeMapper;

    @Resource
    private PictureCommentMapper pictureCommentMapper;

    @Resource
    private LikeService likeService;

    @Resource
    private CommentService commentService;


    @Resource
    private TransactionTemplate transactionTemplate;


    @Override
    public PictureVO uploadPicture(Object source, PictureUploadRequest pictureUploadRequest, User loginUser) {

//        校验用户是否登录
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        // 校验图片是否存在
        Long pictureId = pictureUploadRequest.getId();
        //检查空间id是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {


            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //检查用户是否有权限访问该空间
            ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()) && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE), ErrorCode.NO_AUTH_ERROR, "无权限访问该空间");
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }

        }
        if (pictureId != null) {

            Picture oldPicture = this.getById(pictureId);

            ThrowUtils.throwIf((!loginUser.getId().equals(oldPicture.getUserId()) || !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)), ErrorCode.NO_AUTH_ERROR, "无权限修改图片");
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
                //检查图片是否属于该空间
            } else {
                if (ObjUtil.notEqual(oldPicture.getSpaceId(), spaceId)) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不属于该空间");
                }
            }
        }
//        上传图片 得到文件信息 保存到数据库
        String uploadPathPrefix = getUploadPathPrefix(loginUser, spaceId);
        AbstractFileUploader abstractFileUploader = multipartFileUploader;
        if (source instanceof String) {
            abstractFileUploader = urlFileUploader;
        }
        UploadPictureResult uploadPictureResult = abstractFileUploader.upload(source, uploadPathPrefix);
        // 保存图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setUrl(uploadPictureResult.getUrl());
        // 设置缩略图
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            if (finalSpaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        return PictureVO.objToVo(picture);

    }


    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);


        Long spaceId = oldPicture.getSpaceId();

        if(spaceId != null){
            // 开启事务
            transactionTemplate.execute(status -> {
                // 操作数据库
                boolean result = this.removeById(pictureId);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                // 更新空间的使用额度，释放额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId,spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                return true;
            });
        }else{
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        }

        // 异步清理文件
//        this.clearPictureFile(oldPicture);
    }

    public String getUploadPathPrefix(User loginUser, Long spaceId) {
        String uploadPathPrefix = "";
        if (spaceId != null) {
            uploadPathPrefix = String.format("/space/%s/", spaceId);
        } else {
            uploadPathPrefix = String.format("/pictures/%s/", loginUser.getId());
        }
        return uploadPathPrefix;
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture oldPicture) {
        Long loginUserId = loginUser.getId();
        Long pictureOwnerId = oldPicture.getUserId();
        Long spaceId = oldPicture.getSpaceId();
        boolean isOwner = loginUserId.equals(pictureOwnerId);
        boolean isAdmin = userService.isAdmin(loginUser);

        if (spaceId == null) {
            // 公共图库：必须是图片拥有者或管理员
            ThrowUtils.throwIf(!isOwner && !isAdmin, ErrorCode.NO_AUTH_ERROR, "无权限访问该图片");
        } else {
            // 私人空间：只能是图片拥有者
            ThrowUtils.throwIf(!isOwner, ErrorCode.NO_AUTH_ERROR, "无权限访问该图片");
        }
    }


    /**
     * 获取查询对象
     *
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // and (name like "%xxx%" or introduction like "%xxx%")
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tag like "%\"Java\"%" and like "%\"Python\"%") */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        // 批量聚合点赞数、评论数，并计算当前用户的已点赞集合
        java.util.Set<Long> pictureIdSet = pictureList.stream().map(Picture::getId).collect(java.util.stream.Collectors.toSet());

        // 点赞数聚合
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PictureLike> likeQw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        likeQw.select("pictureId", "COUNT(*) AS cnt")
                .in("pictureId", pictureIdSet)
                .eq("isDelete", 0)
                .groupBy("pictureId");
        java.util.Map<Long, Long> likeCountMap = new java.util.HashMap<>();
        for (java.util.Map<String, Object> row : pictureLikeMapper.selectMaps(likeQw)) {
            Long pid = ((Number) row.get("pictureId")).longValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            likeCountMap.put(pid, cnt);
        }

        // 评论数聚合
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PictureComment> commentQw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        commentQw.select("pictureId", "COUNT(*) AS cnt")
                .in("pictureId", pictureIdSet)
                .eq("isDelete", 0)
                .groupBy("pictureId");
        java.util.Map<Long, Long> commentCountMap = new java.util.HashMap<>();
        for (java.util.Map<String, Object> row : pictureCommentMapper.selectMaps(commentQw)) {
            Long pid = ((Number) row.get("pictureId")).longValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            commentCountMap.put(pid, cnt);
        }

        // 当前用户的已点赞集合
        java.util.Set<Long> likedSet = new java.util.HashSet<>();
        try {
            User curUser = userService.getLoginUser(request);
            if (curUser != null) {
                java.util.List<PictureLike> likedList = pictureLikeMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PictureLike>()
                                .eq(PictureLike::getUserId, curUser.getId())
                                .in(PictureLike::getPictureId, pictureIdSet)
                                .eq(PictureLike::getIsDelete, 0)
                );
                likedSet = likedList.stream().map(PictureLike::getPictureId).collect(java.util.stream.Collectors.toSet());
            }
        } catch (Exception ignored) {}

        // 批量填充到 VO
        for (PictureVO vo : pictureVOList) {
            Long pid = vo.getId();
            vo.setLikeCount(likeCountMap.getOrDefault(pid, 0L));
            vo.setCommentCount(commentCountMap.getOrDefault(pid, 0L));
            vo.setHasLiked(likedSet.contains(pid));
        }

        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        // 如果传递了 url，才校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "图片简介过长");
        }
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，无论是编辑还是创建默认都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //校验图片权限
        this.checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, User loginUser) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        User user = userService.getById(userId);
        UserVO userVO = userService.getUserVO(user);
        pictureVO.setUser(userVO);
        // 统计字段（详情页补充）
        Long pid = picture.getId();
        pictureVO.setLikeCount(likeService.countByPictureId(pid));
        pictureVO.setCommentCount(commentService.countByPictureId(pid));
        pictureVO.setHasLiked(loginUser != null && likeService.hasLiked(pid, loginUser.getId()));
        return pictureVO;
    }


    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();

        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片id不能为空");
        }

        if (pictureReviewRequest.getReviewStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核状态不能为空");
        }

        // 校验审核状态
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(pictureReviewRequest.getReviewStatus());
        ThrowUtils.throwIf(reviewStatusEnum == null, ErrorCode.PARAMS_ERROR, "审核状态不正确");


        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "审核状态未改变，不做操作");
        }
        Picture updatePicture = new Picture();
        updatePicture.setId(id);
        updatePicture.setReviewStatus(reviewStatusEnum.getValue());
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewMessage(pictureReviewRequest.getReviewMessage());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "审核失败");


    }


    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            // codefather.cn?yupi=dog，应该只保留 codefather.cn
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }
}




