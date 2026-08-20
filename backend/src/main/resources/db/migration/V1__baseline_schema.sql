CREATE TABLE auth_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(254) NOT NULL,
    display_name VARCHAR(20) NOT NULL,
    email VARCHAR(254) NULL,
    sudden_nickname VARCHAR(20) NULL,
    ouid VARCHAR(80) NULL,
    profile_image_url VARCHAR(300) NULL,
    clan_none BIT(1) NULL,
    nickname_verified BIT(1) NULL,
    admin BIT(1) NOT NULL,
    verified_at DATETIME(6) NULL,
    email_verification_pending BIT(1) NULL,
    email_verified_at DATETIME(6) NULL,
    account_status ENUM('ACTIVE', 'SUSPENDED', 'BANNED') NULL,
    sanction_reason VARCHAR(500) NULL,
    sanctioned_at DATETIME(6) NULL,
    sanctioned_by_id BIGINT NULL,
    password_salt VARCHAR(32) NOT NULL,
    password_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_auth_users_login_id UNIQUE (login_id),
    CONSTRAINT uk_auth_users_email UNIQUE (email),
    CONSTRAINT uk_auth_users_ouid UNIQUE (ouid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_auth_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE email_verification_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES auth_users (id),
    INDEX idx_password_reset_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mail_daily_usage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usage_date DATE NOT NULL,
    sent_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_mail_daily_usage_date UNIQUE (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE favorite (
    id BIGINT NOT NULL AUTO_INCREMENT,
    auth_user_id BIGINT NULL,
    user_name VARCHAR(30) NOT NULL,
    ouid VARCHAR(64) NULL,
    active_match_query_indexes VARCHAR(64) NULL,
    match_query_profiled_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_favorite_owner FOREIGN KEY (auth_user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE clan_roster_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    auth_user_id BIGINT NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    clan_name VARCHAR(50) NOT NULL,
    ouid VARCHAR(200) NULL,
    stats_match_count INT NULL,
    stats_win_count INT NULL,
    stats_draw_count INT NULL,
    stats_lose_count INT NULL,
    stats_win_rate DOUBLE NULL,
    stats_kill_death_ratio DOUBLE NULL,
    stats_average_kill DOUBLE NULL,
    stats_primary_class VARCHAR(20) NULL,
    stats_combat_type VARCHAR(50) NULL,
    stats_power_score DOUBLE NULL,
    stats_available BIT(1) NULL,
    stats_updated_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_clan_roster_owner_user_name UNIQUE (auth_user_id, user_name),
    CONSTRAINT fk_clan_roster_owner FOREIGN KEY (auth_user_id) REFERENCES auth_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE board_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type ENUM('FREE', 'SUPPORT') NOT NULL,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(20) NOT NULL,
    view_count BIGINT NOT NULL,
    notice BIT(1) NOT NULL,
    support_category ENUM('GENERAL', 'OUID_DISPUTE') NULL,
    support_status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED') NULL,
    claimed_sudden_nickname VARCHAR(20) NULL,
    claimed_ouid VARCHAR(80) NULL,
    claimed_owner_id BIGINT NULL,
    admin_response TEXT NULL,
    resolution_action ENUM('KEEP_EXISTING', 'UNLINK_EXISTING', 'TRANSFER_TO_CLAIMANT', 'REJECT') NULL,
    account_sanction_action ENUM('KEEP', 'ACTIVATE', 'SUSPEND', 'BAN') NULL,
    claimant_verified BIT(1) NULL,
    handled_by_id BIGINT NULL,
    handled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE board_post_images (
    post_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    PRIMARY KEY (post_id, sort_order),
    CONSTRAINT fk_board_post_images_post FOREIGN KEY (post_id) REFERENCES board_posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE support_action_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_analysis_cache (
    normalized_user_name VARCHAR(100) NOT NULL,
    snapshot_hash VARCHAR(64) NOT NULL,
    response_json LONGTEXT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (normalized_user_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rank_progression_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    progression_type ENUM('GRADE', 'SEASON_GRADE') NOT NULL,
    display_order INT NOT NULL,
    rank_group VARCHAR(20) NOT NULL,
    rank_name VARCHAR(40) NOT NULL,
    minimum_experience BIGINT NULL,
    maximum_experience BIGINT NULL,
    best_ranking INT NULL,
    worst_ranking INT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rank_progression_type_order UNIQUE (progression_type, display_order),
    CONSTRAINT uk_rank_progression_type_name UNIQUE (progression_type, rank_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
