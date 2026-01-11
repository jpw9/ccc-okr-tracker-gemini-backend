-- Add manual_progress_set column to key_result table
-- This column tracks whether a KR was manually set (true) or should be calculated from action items (false)

ALTER TABLE key_result ADD COLUMN manual_progress_set BOOLEAN DEFAULT false;
