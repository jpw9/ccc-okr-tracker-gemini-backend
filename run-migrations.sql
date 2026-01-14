-- ============================================================================
-- Run this SQL in your PostgreSQL database (pgAdmin, DBeaver, etc.)
-- to enable the new v1.1.0 Project Access Control migrations
-- ============================================================================

-- Step 1: Remove the changelog entries for v1.1.0 changesets
-- (These were incorrectly marked as "applied" by changelogSync)
DELETE FROM databasechangelog 
WHERE filename LIKE '%v1.1.0%';

-- Step 2: Verify the remaining changesets (should show only v1.0.0)
SELECT id, filename, dateexecuted FROM databasechangelog ORDER BY orderexecuted;

-- After running this SQL, restart your Spring Boot application:
-- mvn spring-boot:run
-- 
-- Liquibase will then create the new tables:
-- - user_projects
-- - role_projects
