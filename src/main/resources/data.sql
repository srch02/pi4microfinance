-- Seed data: run once when DB is empty (enable spring.sql.init.mode=always and spring.jpa.defer-datasource-initialization=true).
-- Admin login: username=admin, password=password

-- 1) Admin user (BCrypt hash for "password")
INSERT INTO ADMIN_USERS (username, email, role, password, enabled, failed_login_attempts, created_at)
SELECT 'admin', 'admin@solidarihealth.local', 'ADMIN',
       '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM ADMIN_USERS WHERE username = 'admin');

-- 2) One group (for memberships / payments)
INSERT INTO groups (name, type, region, join_policy)
SELECT 'Groupe Solidarité Santé', 'solidarity', 'Tunis', 'public'
WHERE NOT EXISTS (SELECT 1 FROM groups WHERE name = 'Groupe Solidarité Santé' LIMIT 1);

-- 3) One member (minimal: CIN, email; optional group link)
INSERT INTO MEMBERS (cin_number, email, age, profession, region, personalized_monthly_price, adherence_score, created_at)
SELECT '12345678', 'member@test.local', 35, 'Enseignant', 'Tunis', 25.00, 80.0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM MEMBERS WHERE cin_number = '12345678');

-- Link member to group if both exist (current_group_id)
UPDATE MEMBERS SET current_group_id = (SELECT id FROM groups WHERE name = 'Groupe Solidarité Santé' LIMIT 1)
WHERE cin_number = '12345678' AND current_group_id IS NULL;
