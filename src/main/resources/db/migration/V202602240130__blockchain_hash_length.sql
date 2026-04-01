-- Increase blockchain_hash column to fit Hedera transaction IDs (all tables)
ALTER TABLE CLAIMS MODIFY COLUMN blockchain_hash VARCHAR(256) NULL;
ALTER TABLE PAYMENTS MODIFY COLUMN blockchain_hash VARCHAR(256) NULL;
ALTER TABLE BLOCKCHAIN_TRANSACTIONS MODIFY COLUMN blockchain_hash VARCHAR(256) NULL;
