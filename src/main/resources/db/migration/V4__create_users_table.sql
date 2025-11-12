-- V4__create_users_table.sql
CREATE TABLE IF NOT EXISTS `users` (
                                       `id`         BIGINT NOT NULL AUTO_INCREMENT,
                                       `username`   VARCHAR(50)  NOT NULL,
    `password`   VARCHAR(255) NOT NULL,
    `email`      VARCHAR(100) NOT NULL,
    `nickname`   VARCHAR(50)  NULL,
    `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    UNIQUE KEY `uk_users_email`    (`email`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
