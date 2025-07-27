package com.zjl.lqpicturebackend.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjl.lqpicturebackend.model.Picture;
import com.zjl.lqpicturebackend.model.User;
import com.zjl.lqpicturebackend.model.dto.picture.PictureEditRequest;
import com.zjl.lqpicturebackend.model.dto.picture.PictureQueryRequest;
import com.zjl.lqpicturebackend.model.dto.picture.PictureReviewRequest;
import com.zjl.lqpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zjl.lqpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;


/**
 * @author zou
 * @description 针对表【picture】的数据库操作Service
 * @createDate 2025-06-12 21:51:36
 */
public interface PictureService extends IService<Picture> {

    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

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

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);


    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);





}
