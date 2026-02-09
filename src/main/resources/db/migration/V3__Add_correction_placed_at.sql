ALTER TABLE corrections ADD COLUMN correction_placed_at TIMESTAMP WITH TIME ZONE;

UPDATE corrections
SET correction_placed_at = saved_at AT TIME ZONE 'UTC'
WHERE correction_placed_at IS NULL;

ALTER TABLE corrections ALTER COLUMN correction_placed_at SET NOT NULL;
