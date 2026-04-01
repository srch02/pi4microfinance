-- Telegram notifications support:
-- - store member telegram chat id
-- - store receiver/read status for direct messages
-- - store payment timestamp
-- - store membership reminder timestamp

ALTER TABLE MEMBERS
    ADD COLUMN telegram_chat_id VARCHAR(64) NULL;

ALTER TABLE DIRECT_MESSAGES
    ADD COLUMN receiver_member_id BIGINT NOT NULL,
    ADD COLUMN read_at DATETIME NULL;

ALTER TABLE PAYMENTS
    ADD COLUMN created_at DATETIME NULL;

ALTER TABLE MEMBERSHIPS
    ADD COLUMN last_payment_reminder_at DATETIME NULL;

