ALTER TABLE governance.claim_ledger_item
  ADD COLUMN claim_value_text text;

CREATE INDEX claim_ledger_org_source_span_idx
  ON governance.claim_ledger_item (organization_id, source_span_ref);

COMMENT ON COLUMN governance.claim_ledger_item.claim_value_text IS
  'Task 5C optional claim value text for governed-intake bridge output. A populated value remains a claim, not a canonical fact.';
COMMENT ON INDEX governance.claim_ledger_org_source_span_idx IS
  'Task 5C governed-intake bridge lookup index for deterministic source_span_ref idempotency checks. This does not make ClaimLedger canonical fact storage.';
