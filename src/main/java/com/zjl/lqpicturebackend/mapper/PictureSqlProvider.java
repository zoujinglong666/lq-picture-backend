package com.zjl.lqpicturebackend.mapper;

import org.apache.ibatis.jdbc.SQL;

public class PictureSqlProvider {

    public String listMyLikedPicturesV3() {
        return new SQL() {{
            SELECT("p.*");
            FROM("picture p");
            JOIN("picture_like pl ON p.id = pl.pictureId");
            WHERE("pl.userId = #{userId}");
            WHERE("pl.isDelete = 0");
            WHERE("p.isDelete = 0");
            ORDER_BY("pl.createTime DESC");
        }}.toString();
    }
}