package com.zjl.lqpicturebackend.model.dto.space;

import com.zjl.lqpicturebackend.common.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 查询空间请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor  // ✅ 无参构造，Jackson 需要
@AllArgsConstructor // 可选，全参构造，方便手动 new
public class SpaceQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

    private static final long serialVersionUID = 1L;
}
