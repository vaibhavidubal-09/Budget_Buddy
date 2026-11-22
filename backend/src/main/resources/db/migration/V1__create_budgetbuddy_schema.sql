CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    description VARCHAR(120) NOT NULL,
    category VARCHAR(60) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    date DATE NOT NULL,
    split_details TEXT NULL,
    PRIMARY KEY (id),
    INDEX idx_expenses_user_date (user_id, date)
) ENGINE=InnoDB;

CREATE TABLE income_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    source VARCHAR(80) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    date DATE NOT NULL,
    frequency VARCHAR(16) NOT NULL DEFAULT 'ONCE',
    interval_days INT NULL,
    PRIMARY KEY (id),
    INDEX idx_income_user_date (user_id, date)
) ENGINE=InnoDB;

CREATE TABLE category_budgets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    category VARCHAR(60) NOT NULL,
    budget_limit INT NOT NULL,
    period VARCHAR(12) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_budgets_user_period_category (user_id, period, category)
) ENGINE=InnoDB;
