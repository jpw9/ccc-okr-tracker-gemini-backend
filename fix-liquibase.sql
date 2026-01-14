-- Fix Liquibase synchronization issue
-- Option 1: Reset Liquibase tracking only (keeps your data)
-- Run this if you want to keep existing data but fix Liquibase

DROP TABLE IF EXISTS public.databasechangeloglock CASCADE;
DROP TABLE IF EXISTS public.databasechangelog CASCADE;

-- After running this, restart the Spring Boot application
-- Liquibase will recreate the changelog tables and mark existing schema as applied

-- ================================================================================
-- Option 2: Complete reset (⚠️ DELETES ALL DATA - use only if needed)
-- Uncomment the lines below to drop everything and start fresh
-- ================================================================================

-- DROP SCHEMA public CASCADE;
-- CREATE SCHEMA public;
-- GRANT ALL ON SCHEMA public TO postgres;
-- GRANT ALL ON SCHEMA public TO public;
