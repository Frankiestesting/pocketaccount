INSERT INTO receipt_waiver_reason (code, label, sort_order)
VALUES ('SAVINGS', 'Sparing', 6)
ON CONFLICT (code) DO NOTHING;
