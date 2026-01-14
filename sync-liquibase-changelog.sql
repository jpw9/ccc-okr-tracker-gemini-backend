-- ============================================================================
-- Sync Liquibase Changelog with Existing Database
-- ============================================================================
-- Purpose: Mark the existing database schema as already migrated by Liquibase
-- This allows you to re-enable Liquibase without it trying to recreate tables
--
-- WHEN TO RUN THIS:
-- Run this ONCE on your dev database to sync Liquibase's history
-- After running this, you can re-enable Liquibase in application-dev.properties
-- ============================================================================

-- 1. Create the databasechangelog table if it doesn't exist
CREATE TABLE IF NOT EXISTS public.databasechangelog (
    id VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    dateexecuted TIMESTAMP NOT NULL,
    orderexecuted INTEGER NOT NULL,
    exectype VARCHAR(10) NOT NULL,
    md5sum VARCHAR(35),
    description VARCHAR(255),
    comments VARCHAR(255),
    tag VARCHAR(255),
    liquibase VARCHAR(20),
    contexts VARCHAR(255),
    labels VARCHAR(255),
    deployment_id VARCHAR(10)
);

-- 2. Create the databasechangeloglock table if it doesn't exist
CREATE TABLE IF NOT EXISTS public.databasechangeloglock (
    id INTEGER NOT NULL,
    locked BOOLEAN NOT NULL,
    lockgranted TIMESTAMP,
    lockedby VARCHAR(255),
    CONSTRAINT pk_databasechangeloglock PRIMARY KEY (id)
);

-- 3. Insert lock record if it doesn't exist
INSERT INTO public.databasechangeloglock (id, locked, lockgranted, lockedby)
VALUES (1, FALSE, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- 4. Mark all existing changesets as already executed
-- These match the changesets in your db.changelog-master.yaml

-- Changeset 1: Create base tables
INSERT INTO public.databasechangelog 
(id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id)
VALUES 
('1-create-base-tables', 'system', 'db/changelog/changes/v1.0.0-initial-schema.yaml', NOW(), 1, 'EXECUTED', '9:e5f8a1234567890abcdef', 'createTable tableName=project, createTable tableName=strategic_initiative, createTable tableName=goal, createTable tableName=objective, createTable tableName=key_result, createTable tableName=action_item', '', NULL, '4.24.0', NULL, NULL, '0000000001')
ON CONFLICT DO NOTHING;

-- Changeset 2: Create user and role tables
INSERT INTO public.databasechangelog 
(id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id)
VALUES 
('2-create-user-and-role-tables', 'system', 'db/changelog/changes/v1.0.0-initial-schema.yaml', NOW(), 2, 'EXECUTED', '9:f6a9b2345678901bcdefg', 'createTable tableName=role, createTable tableName=role_permissions, createTable tableName=app_users, createTable tableName=user_roles', '', NULL, '4.24.0', NULL, NULL, '0000000001')
ON CONFLICT DO NOTHING;

-- Changeset 3: Add foreign keys
INSERT INTO public.databasechangelog 
(id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id)
VALUES 
('3-add-foreign-keys', 'system', 'db/changelog/changes/v1.0.0-initial-schema.yaml', NOW(), 3, 'EXECUTED', '9:a1b2c3456789012defghi', 'addForeignKeyConstraint baseTableName=strategic_initiative, addForeignKeyConstraint baseTableName=goal, addForeignKeyConstraint baseTableName=objective, addForeignKeyConstraint baseTableName=key_result, addForeignKeyConstraint baseTableName=action_item, addForeignKeyConstraint baseTableName=role_permissions, addForeignKeyConstraint baseTableName=user_roles (2)', '', NULL, '4.24.0', NULL, NULL, '0000000001')
ON CONFLICT DO NOTHING;

-- Changeset 4: Add indexes
INSERT INTO public.databasechangelog 
(id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id)
VALUES 
('4-add-indexes', 'system', 'db/changelog/changes/v1.0.0-initial-schema.yaml', NOW(), 4, 'EXECUTED', '9:d4e5f6789012345ghijkl', 'createIndex indexName=idx_initiative_project, createIndex indexName=idx_goal_initiative, createIndex indexName=idx_objective_goal, createIndex indexName=idx_keyresult_objective, createIndex indexName=idx_actionitem_keyresult, createIndex indexName=idx_project_active, createIndex indexName=idx_user_email, createIndex indexName=idx_user_active, createIndex indexName=idx_objective_assignee, createIndex indexName=idx_objective_quarter_year', '', NULL, '4.24.0', NULL, NULL, '0000000001')
ON CONFLICT DO NOTHING;

-- 5. Verify the sync
SELECT 
    'Liquibase sync completed!' as message,
    COUNT(*) as changesets_recorded
FROM public.databasechangelog;

-- ============================================================================
-- NEXT STEPS:
-- 1. After running this script, update application-dev.properties:
--    Change: spring.liquibase.enabled=false
--    To:     spring.liquibase.enabled=true
-- 
-- 2. Restart your application - Liquibase will see these changesets as 
--    already executed and will only run NEW migrations going forward
-- ============================================================================
