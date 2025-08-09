package com.zjl.lqpicturebackend.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjl.lqpicturebackend.model.Picture;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.dto.picture.*;
import com.zjl.lqpicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;


/**
 * @author zou
 * @description 针对表【picture】的数据库操作Service
 * @createDate 2025-06-12 21:51:36
 */
public interface PictureService extends IService<Picture> {

    PictureVO uploadPicture(Object source, PictureUploadRequest pictureUploadRequest, User loginUser);

    void deletePicture(long pictureId, User loginUser);

    /**
     * 检查图片权限
     * @param loginUser
     * @param oldPicture
     */
    void checkPictureAuth(User loginUser, Picture oldPicture);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    PictureVO getPictureVO(Picture picture, User loginUser);


    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);


    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);
}
