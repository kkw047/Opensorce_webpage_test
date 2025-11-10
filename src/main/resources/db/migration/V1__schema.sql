-- V1__schema.sql  (MySQL 8.x, utf8mb4)
SET NAMES utf8mb4;
SET time_zone = '+09:00';

CREATE TABLE IF NOT EXISTS `users` (
                                       `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       `email` VARCHAR(255) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `categories` (
                                            `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            `name` VARCHAR(50) NOT NULL UNIQUE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `region_kor` (
                                            `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            `region_do` VARCHAR(50)  NOT NULL,
    `region_si` VARCHAR(80)  NOT NULL,
    UNIQUE KEY `uq_region_do_si` (`region_do`,`region_si`),
    KEY `idx_region_do` (`region_do`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `email_verification` (
                                                    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                    `email` VARCHAR(255) NOT NULL UNIQUE,
    `code` VARCHAR(10)  NOT NULL,
    `expires_at` DATETIME  NOT NULL,
    `verified` TINYINT(1) NOT NULL DEFAULT 0
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ✅ 예약어이므로 반드시 백틱 사용
CREATE TABLE IF NOT EXISTS `groups` (
                                        `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        `owner_id` BIGINT     NOT NULL,
                                        `name` VARCHAR(100)   NOT NULL,
    `promo_text` VARCHAR(255),
    `region_do` VARCHAR(50)  NOT NULL,
    `region_si` VARCHAR(80)  NOT NULL,
    `image_url` VARCHAR(255),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_groups_owner` FOREIGN KEY (`owner_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `group_categories` (
                                                  `group_id` BIGINT   NOT NULL,
                                                  `category_id` BIGINT NOT NULL,
                                                  PRIMARY KEY (`group_id`,`category_id`),
    CONSTRAINT `fk_gc_group` FOREIGN KEY (`group_id`)   REFERENCES `groups`(`id`)     ON DELETE CASCADE,
    CONSTRAINT `fk_gc_cat`   FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_categories` (
                                                 `user_id` BIGINT    NOT NULL,
                                                 `category_id` BIGINT NOT NULL,
                                                 PRIMARY KEY (`user_id`,`category_id`),
    CONSTRAINT `fk_uc_user` FOREIGN KEY (`user_id`)    REFERENCES `users`(`id`)      ON DELETE CASCADE,
    CONSTRAINT `fk_uc_cat`  FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
