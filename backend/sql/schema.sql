-- ===============================================================
-- Project: Seichou-Logos (成長の軌跡 / 成长之理)
-- Database: PostgreSQL
-- Description: Database schema for personal growth quantification
-- ===============================================================

-- 1. Users Table (用户表)
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE, -- 通常为邮箱
    password TEXT NOT NULL,
    role VARCHAR(20) DEFAULT 'USER', -- USER, ADMIN
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. User Stats Table (用户数值表)
CREATE TABLE user_stats (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    level INTEGER DEFAULT 1,
    physical_exp INTEGER DEFAULT 0,
    mental_exp INTEGER DEFAULT 0,
    max_exp INTEGER DEFAULT 100,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. User Preferences Table (用户偏好表)
CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    theme VARCHAR(20) DEFAULT 'dark',
    language VARCHAR(10) DEFAULT 'en',
    ai_personality VARCHAR(30) DEFAULT 'empathetic',
    notifications_enabled BOOLEAN DEFAULT TRUE,
    data_privacy VARCHAR(20) DEFAULT 'standard',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Daily Logs Table (日常记录表)
CREATE TABLE daily_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    content TEXT NOT NULL, -- 用户输入的原始记录
    mood_score INTEGER CHECK (mood_score >= 1 AND mood_score <= 10), -- 1-10 情绪自评分
    log_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_log_date UNIQUE (user_id, log_date)
);

-- 5. Emotion Analysis Table (情绪重构表)
CREATE TABLE emotion_analysis (
    analysis_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    log_id UUID NOT NULL REFERENCES daily_logs(log_id) ON DELETE CASCADE,
    primary_emotion VARCHAR(50), -- 识别出的核心情绪 (如: 焦虑, 愤怒)
    reframed_insight TEXT, -- AI 重构后的核心洞察
    growth_assets JSONB NOT NULL, -- 存储转化的成长数值 (e.g., {"self_awareness": 10, "stress_metabolism": 5})
    ai_insight_details JSONB NOT NULL, -- 存储 AI 拆解的心理反馈、背后的真实需求等非结构化数据
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Board Cards Table (看板任务表)
CREATE TABLE board_cards (
    card_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    time_frame VARCHAR(20) NOT NULL, -- today, week, month
    dimension VARCHAR(20) NOT NULL, -- physical, mental
    status VARCHAR(20) DEFAULT 'todo', -- todo, done
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Board Card Tags Table (看板任务标签表)
CREATE TABLE board_card_tags (
    card_id UUID NOT NULL REFERENCES board_cards(card_id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (card_id, tag)
);

-- 8. Chat Messages Table (智聊室历史表)
CREATE TABLE chat_messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- user, assistant
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_daily_logs_user_id ON daily_logs(user_id);
CREATE INDEX idx_emotion_analysis_log_id ON emotion_analysis(log_id);
CREATE INDEX idx_emotion_analysis_growth_assets ON emotion_analysis USING GIN (growth_assets);
CREATE INDEX idx_board_cards_user_id ON board_cards(user_id);
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
