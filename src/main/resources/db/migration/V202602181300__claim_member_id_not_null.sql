-- Ensure claims are always linked to a member.
-- If you have existing claims with member_id IS NULL, run first:
-- UPDATE CLAIMS SET member_id = (SELECT id FROM MEMBERS LIMIT 1) WHERE member_id IS NULL;

ALTER TABLE CLAIMS MODIFY COLUMN member_id BIGINT NOT NULL;
