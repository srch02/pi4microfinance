-- Option B: link AdminUser (portal account) to Member (insurance business)
-- Run after schema exists for ADMIN_USERS and MEMBERS.

ALTER TABLE ADMIN_USERS ADD COLUMN member_id BIGINT NULL;
ALTER TABLE ADMIN_USERS ADD CONSTRAINT fk_admin_users_member
    FOREIGN KEY (member_id) REFERENCES MEMBERS(id);
