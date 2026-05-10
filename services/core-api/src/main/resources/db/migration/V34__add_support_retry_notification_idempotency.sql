-- Task 56 support retry idempotency guard.

CREATE UNIQUE INDEX notification_support_retry_source_ref_uidx
  ON operations.notification (organization_id, source_ref)
  WHERE source_ref IS NOT NULL
    AND source_ref LIKE 'support_retry:%';
