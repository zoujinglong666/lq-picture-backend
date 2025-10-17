package com.zjl.lqpicturebackend.model.vo;

import com.zjl.lqpicturebackend.model.PictureComment;
import com.zjl.lqpicturebackend.model.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class CommentVO implements Serializable {

    private Long id;
    private Long pictureId;
    private Long userId;
    private String content;
    private java.util.Date createTime;

    private UserVO user;
    private List<CommentVO> replies = new ArrayList<>();

    private static final long serialVersionUID = 1L;

    public static CommentVO from(PictureComment c, Map<Long, User> userMap, List<PictureComment> replyList) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(c, vo);
        User u = userMap.get(c.getUserId());
        if (u != null) {
            vo.setUser(toUserVO(u));
        }
        if (replyList != null) {
            for (PictureComment rc : replyList) {
                CommentVO rvo = new CommentVO();
                BeanUtils.copyProperties(rc, rvo);
                User ru = userMap.get(rc.getUserId());
                if (ru != null) {
                    rvo.setUser(toUserVO(ru));
                }
                vo.getReplies().add(rvo);
            }
        }
        return vo;
    }

    private static UserVO toUserVO(User u) {
        UserVO uv = new UserVO();
        org.springframework.beans.BeanUtils.copyProperties(u, uv);
        return uv;
    }
}