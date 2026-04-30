-- ============================================================================
-- V14: Add composite FK job(company_id, organization_id) -> company
--
-- V12 hardened all child -> parent FK chains except job -> company.
-- job.company_id references recruiting.company(company_id) via a simple FK
-- from V10. This allows a job row in org A to reference a company row in org B,
-- bypassing multi-tenancy isolation at the database constraint layer.
--
-- V12 already added uq_company_id_org on recruiting.company (line 31-34) and
-- uq_job_id_org on recruiting.job (line 36-39), so all prerequisites for the
-- composite FK are in place.
--
-- The index job_org_company_idx on recruiting.job (organization_id, company_id)
-- from V10 (line 226-227) supports the composite FK without a new index.
-- ============================================================================

-- Step 1: Add the composite FK
ALTER TABLE recruiting.job
  ADD CONSTRAINT fk_job_company_org
    FOREIGN KEY (company_id, organization_id)
    REFERENCES recruiting.company (company_id, organization_id);
COMMENT ON CONSTRAINT fk_job_company_org ON recruiting.job IS
  'V14: composite FK ensuring job.company org matches company org.';

-- Step 2: Drop the now-redundant simple FK (V10 inline REFERENCES, auto-named)
ALTER TABLE recruiting.job
  DROP CONSTRAINT IF EXISTS job_company_id_fkey;
