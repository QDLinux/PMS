-- MySQL 5.5.20 initialization script
-- Create database first:
-- CREATE DATABASE personal_manager DEFAULT CHARACTER SET utf8;

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS accounting_record;
DROP TABLE IF EXISTS plan_item;
DROP TABLE IF EXISTS goal;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  nickname VARCHAR(50) DEFAULT NULL,
  email VARCHAR(100) DEFAULT NULL,
  avatar_path VARCHAR(255) DEFAULT NULL,
  password VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE accounting_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  category VARCHAR(50) NOT NULL,
  account_date DATETIME NOT NULL,
  note VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_accounting_owner_date (owner_id, account_date),
  KEY idx_accounting_owner_created (owner_id, created_at),
  CONSTRAINT fk_accounting_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE plan_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(1000) DEFAULT NULL,
  start_date DATE DEFAULT NULL,
  end_date DATE DEFAULT NULL,
  priority VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_plan_owner_status (owner_id, status),
  KEY idx_plan_owner_end (owner_id, end_date),
  KEY idx_plan_owner_created (owner_id, created_at),
  CONSTRAINT fk_plan_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE goal (
  id BIGINT NOT NULL AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) DEFAULT NULL,
  target_amount DECIMAL(12,2) NOT NULL,
  current_amount DECIMAL(12,2) NOT NULL,
  start_date DATE DEFAULT NULL,
  deadline DATE DEFAULT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_goal_owner_status (owner_id, status),
  KEY idx_goal_owner_deadline (owner_id, deadline),
  KEY idx_goal_owner_created (owner_id, created_at),
  CONSTRAINT fk_goal_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- username: admin
-- password: admin123456
INSERT INTO sys_user (username, nickname, email, avatar_path, password, created_at)
VALUES ('admin', 'Admin', NULL, NULL, '{noop}admin123456', NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- 为已有库补建缺失索引（若已建则忽略错误）
ALTER TABLE plan_item ADD INDEX IF NOT EXISTS idx_plan_owner_created (owner_id, created_at);
ALTER TABLE goal ADD INDEX IF NOT EXISTS idx_goal_owner_created (owner_id, created_at);
