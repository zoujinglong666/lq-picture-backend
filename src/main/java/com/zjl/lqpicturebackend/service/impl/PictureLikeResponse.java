package com.zjl.lqpicturebackend.service.impl;

public class PictureLikeResponse {
    private int likeCount;
    private boolean liked;

    public PictureLikeResponse(int likeCount, boolean liked) {
        this.likeCount = likeCount;
        this.liked = liked;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }
}
