CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL COMMENT '建议使用 BCrypt 加密存储',
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    role TINYINT DEFAULT 0 COMMENT '0-普通用户 1-管理员',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `book` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(100),
    cover VARCHAR(500),
    publish_year INT,
    isbn VARCHAR(20),
    ol_id VARCHAR(100),
    description TEXT,
    tags VARCHAR(255),
    imported TINYINT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT DEFAULT 0,
    KEY idx_book_ol_id (ol_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `reading_plan` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0 未开始 1 阅读中 2 已读完',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_book (user_id, book_id),
    KEY idx_reading_plan_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `note` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    title VARCHAR(255),
    content TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT DEFAULT 0,
    KEY idx_note_user (user_id),
    KEY idx_note_book (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `comment` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    content TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT DEFAULT 0,
    KEY idx_comment_note (note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `import_candidate` (
    ol_id VARCHAR(100) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(100),
    first_publish_year INT,
    cover VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
