package com.zjl.lqpicturebackend.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjl.lqpicturebackend.model.Picture;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * @author zou
 */
public interface PictureMapper extends BaseMapper<Picture> {

    Page<Picture> listMyLikedPicturesByMyBatis(Page<Picture> page, @Param("userId") Long userId);

    @SelectProvider(type = PictureSqlProvider.class, method = "listMyLikedPicturesV3")
    Page<Picture> listMyLikedPicturesV3(Page<Picture> page, @Param("userId") Long userId);
}




