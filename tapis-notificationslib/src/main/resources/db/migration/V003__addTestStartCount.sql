-- Add column for recording the number of events requested when starting a test sequence.
ALTER TABLE notifications_tests ADD COLUMN IF NOT EXISTS start_count INTEGER NOT NULL DEFAULT 0;
