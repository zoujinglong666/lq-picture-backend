package com.zjl.lqpicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.zjl.lqpicturebackend.model.PictureLike;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper for picture_like
 */
public interface PictureLikeMapper extends BaseMapper<PictureLike> {

    /**
     * 查询包含已逻辑删除的记录（绕过逻辑删除拦截）
     */
    @Select("SELECT * FROM picture_like WHERE pictureId = #{pictureId} AND userId = #{userId} LIMIT 1")
    PictureLike selectOneIncludeDeleted(@Param("pictureId") Long pictureId, @Param("userId") Long userId);

    /**
     * 恢复点赞：将 is_delete 从 1 改为 0，并更新 create_time（绕过逻辑删除拦截）
     */
    @Update("UPDATE picture_like SET isDelete = 0, createTime = NOW() WHERE id = #{id}")
    int restoreLike(@Param("id") Long id);
}