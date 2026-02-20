-- Migration: ajout des colonnes d'authentification pour MEMBERS
-- Date: 2026-02-18

-- ATTENTION: Assurez-vous que ces colonnes n'existent pas déjà avant d'exécuter cette migration.
-- Version compatible MySQL (sans IF NOT EXISTS).

ALTER TABLE MEMBERS ADD COLUMN email VARCHAR(255);
ALTER TABLE MEMBERS ADD COLUMN password VARCHAR(255);
ALTER TABLE MEMBERS ADD COLUMN enabled TINYINT(1) DEFAULT 1;
ALTER TABLE MEMBERS ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE MEMBERS ADD COLUMN locked_at DATETIME NULL;
ALTER TABLE MEMBERS ADD COLUMN last_login DATETIME NULL;
ALTER TABLE MEMBERS ADD COLUMN created_at DATETIME NULL;

-- Contrainte d'unicité sur l'email
ALTER TABLE MEMBERS ADD CONSTRAINT uq_members_email UNIQUE (email);
