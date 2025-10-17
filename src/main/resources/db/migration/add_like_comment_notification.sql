-- 点赞表
CREATE TABLE IF NOT EXISTS picture_like (
  id BIGINT PRIMARY KEY,
  pictureId BIGINT NOT NULL,
  userId BIGINT NOT NULL,
  createTime DATETIME DEFAULT CURRENT_TIMESTAMP,
  isDelete TINYINT DEFAULT 0,
  UNIQUE KEY uq_user_picture (userId, pictureId, isDelete),
  INDEX idx_picture (pictureId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评论表（支持二级）
CREATE TABLE IF NOT EXISTS picture_comment (
  id BIGINT PRIMARY KEY,
  pictureId BIGINT NOT NULL,
  userId BIGINT NOT NULL,
  parentId BIGINT NULL,
  content VARCHAR(1000) NOT NULL,
  createTime DATETIME DEFAULT CURRENT_TIMESTAMP,
  isDelete TINYINT DEFAULT 0,
  INDEX idx_picture_time (pictureId, createTime),
  INDEX idx_parent (parentId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知表
CREATE TABLE IF NOT EXISTS notification (
  id BIGINT PRIMARY KEY,
  userId BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  refId BIGINT NULL,
  pictureId BIGINT NOT NULL,
  content VARCHAR(255),
  readStatus TINYINT DEFAULT 0,
  createTime DATETIME DEFAULT CURRENT_TIMESTAMP,
  isDelete TINYINT DEFAULT 0,
  INDEX idx_user_read (userId, readStatus, createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;