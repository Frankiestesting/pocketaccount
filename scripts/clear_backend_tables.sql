-- Clear backend tables only (no system tables).
-- Uses RESTART IDENTITY to reset sequences and CASCADE to satisfy FKs.

TRUNCATE TABLE
  receipt_match,
  receipt,
  statement_transactions,
  interpretation_results,
  interpretation_jobs,
  correction_history,
  jobs,
  bank_transaction,
  account,
  documents
RESTART IDENTITY CASCADE;
