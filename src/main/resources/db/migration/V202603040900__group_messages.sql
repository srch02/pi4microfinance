-- Group chat messages: encrypted content + Hedera audit fields + optional fraud flags

CREATE TABLE GROUP_MESSAGES (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    sender_member_id BIGINT NOT NULL,
    encrypted_content TEXT NOT NULL,
    message_hash VARCHAR(64) NOT NULL,
    hedera_tx_hash VARCHAR(256) NULL,
    fraud_score DECIMAL(5,2) NULL,
    flagged BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_group_messages_group FOREIGN KEY (group_id) REFERENCES groups(id),
    CONSTRAINT fk_group_messages_member FOREIGN KEY (sender_member_id) REFERENCES MEMBERS(id)
);

