-- Hedera blockchain integration: new columns and table

-- Member: wallet, coins, contract hash
ALTER TABLE MEMBERS ADD COLUMN wallet_address VARCHAR(256) NULL;
ALTER TABLE MEMBERS ADD COLUMN coin_balance FLOAT NULL;
ALTER TABLE MEMBERS ADD COLUMN blockchain_contract_hash VARCHAR(128) NULL;

-- Payment: blockchain hash, coin/dt amounts
ALTER TABLE PAYMENTS ADD COLUMN blockchain_hash VARCHAR(256) NULL;
ALTER TABLE PAYMENTS ADD COLUMN coin_amount FLOAT NULL;
ALTER TABLE PAYMENTS ADD COLUMN dt_amount FLOAT NULL;

-- Claim: blockchain hash, reimbursement coins
ALTER TABLE CLAIMS ADD COLUMN blockchain_hash VARCHAR(256) NULL;
ALTER TABLE CLAIMS ADD COLUMN reimbursement_coins DECIMAL(19,4) NULL;

-- Blockchain transaction audit table
CREATE TABLE BLOCKCHAIN_TRANSACTIONS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blockchain_hash VARCHAR(256) NULL,
    transaction_type VARCHAR(64) NOT NULL,
    coins_transferred DECIMAL(19,4) NULL,
    member_id BIGINT NULL,
    payment_id BIGINT NULL,
    claim_id BIGINT NULL,
    created_at DATETIME NULL,
    FOREIGN KEY (member_id) REFERENCES MEMBERS(id)
);
