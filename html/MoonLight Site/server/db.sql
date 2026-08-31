-- ============================================================
-- MoonLight Visual — схема базы данных (MySQL 5.5+)
-- Выполните этот скрипт в вашей базе (phpMyAdmin или клиент)
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(191) NOT NULL,
  email VARCHAR(191) NOT NULL UNIQUE,
  password VARCHAR(191) NOT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sessions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(191) NOT NULL UNIQUE,
  user_email VARCHAR(191) NOT NULL,
  created_at DATETIME NOT NULL,
  FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS codes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  tier ENUM('premium','pro') NOT NULL DEFAULT 'premium',
  duration INT NOT NULL DEFAULT 30,
  note VARCHAR(191) NULL,
  created_by VARCHAR(191) NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS redeemed_codes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_email VARCHAR(191) NOT NULL,
  code VARCHAR(64) NOT NULL,
  tier ENUM('premium','pro') NOT NULL DEFAULT 'premium',
  activated_at DATETIME NOT NULL,
  expires_at DATETIME NOT NULL,
  FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE,
  UNIQUE KEY uniq_user_code (user_email, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS owners (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(191) NOT NULL UNIQUE,
  activated_at DATETIME NOT NULL,
  FOREIGN KEY (email) REFERENCES users(email) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Предустановленные стартовые коды
-- ============================================================
INSERT INTO codes (code, tier, duration, note, created_by, created_at) VALUES
  ('MOONLIGHT2026', 'premium', 30,  'Стартовый код', 'system', NOW()),
  ('VISUALPRO',     'pro',     90,  'Pro на 3 месяца', 'system', NOW()),
  ('MINECRAFT',     'premium', 14,  'Premium на 2 недели', 'system', NOW()),
  ('STARLIGHT',     'pro',     30,  'Pro на месяц', 'system', NOW()),
  ('BLOCKCRAFT',    'premium', 60,  'Premium на 2 месяца', 'system', NOW()),
  ('NIGHTOWL',      'pro',     365, 'Pro на год', 'system', NOW());
