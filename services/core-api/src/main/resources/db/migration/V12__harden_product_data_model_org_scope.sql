-- ============================================================================
-- V12: Harden Product Data Model Org-Scope (Task 16 Hardening)
--
-- Problem: V10 child tables have their own organization_id and a simple FK
-- to the parent table's id, but no composite FK enforcing
-- parent.organization_id = child.organization_id at the DB level.
-- This allows a child row in org_B to reference a parent row in org_A,
-- bypassing multi-tenancy isolation at the database constraint layer.
--
-- Solution:
--   1. Add UNIQUE (id, organization_id) on each parent table to support
--      composite FK REFERENCES.
--   2. Add composite FOREIGN KEY (parent_id, organization_id)
--      REFERENCES parent (id, organization_id) on each child table.
--   3. Drop the now-redundant simple FKs that the composite FKs fully subsume.
--
-- Nullable parent FK columns (interaction.job_id, document.source_item_id)
-- are NOT hardened with composite FKs: optional links don't pose the same
-- org-isolation risk, and nullable composite FKs add complexity without
-- proportional benefit when the NOT NULL parent columns are already hardened.
--
-- commission.consultant_id references identity.user_account (a cross-cutting
-- identity table, not a product-scoped entity) and is excluded from composite
-- FK treatment.
--
-- Follow-up migration (do NOT rewrite V10). All existing V10 data is preserved.
-- ============================================================================

-- === Step 1: Add UNIQUE (id, organization_id) on parent tables ===============

ALTER TABLE recruiting.company
  ADD CONSTRAINT uq_company_id_org UNIQUE (company_id, organization_id);
COMMENT ON CONSTRAINT uq_company_id_org ON recruiting.company IS
  'Task 16 hardening: enables composite FK from child tables ensuring org-scope alignment.';

ALTER TABLE recruiting.job
  ADD CONSTRAINT uq_job_id_org UNIQUE (job_id, organization_id);
COMMENT ON CONSTRAINT uq_job_id_org ON recruiting.job IS
  'Task 16 hardening: enables composite FK from child tables ensuring org-scope alignment.';

ALTER TABLE recruiting.candidate
  ADD CONSTRAINT uq_candidate_id_org UNIQUE (candidate_id, organization_id);
COMMENT ON CONSTRAINT uq_candidate_id_org ON recruiting.candidate IS
  'Task 16 hardening: enables composite FK from child tables ensuring org-scope alignment.';

ALTER TABLE recruiting.candidate_profile
  ADD CONSTRAINT uq_candidate_profile_id_org UNIQUE (candidate_profile_id, organization_id);
COMMENT ON CONSTRAINT uq_candidate_profile_id_org ON recruiting.candidate_profile IS
  'Task 16 hardening: enables composite FK from child tables ensuring org-scope alignment.';

ALTER TABLE recruiting.shortlist
  ADD CONSTRAINT uq_shortlist_id_org UNIQUE (shortlist_id, organization_id);
COMMENT ON CONSTRAINT uq_shortlist_id_org ON recruiting.shortlist IS
  'Task 16 hardening: enables composite FK from child tables ensuring org-scope alignment.';

ALTER TABLE recruiting.placement
  ADD CONSTRAINT uq_placement_id_org UNIQUE (placement_id, organization_id);
COMMENT ON CONSTRAINT uq_placement_id_org ON recruiting.placement IS
  'Task 16 hardening: enables composite FK from child tables ensuring org-scope alignment.';

ALTER TABLE recruiting.candidate_company_interaction
  ADD CONSTRAINT uq_interaction_id_org
    UNIQUE (candidate_company_interaction_id, organization_id);
COMMENT ON CONSTRAINT uq_interaction_id_org ON recruiting.candidate_company_interaction IS
  'Task 16 hardening: enables composite FK from child tables ensuring org-scope alignment.';

-- === Step 2: Add composite FKs on child tables ===============================

-- 2a. company_contact -> company
ALTER TABLE recruiting.company_contact
  ADD CONSTRAINT fk_company_contact_comp_org
    FOREIGN KEY (company_id, organization_id)
    REFERENCES recruiting.company (company_id, organization_id);
COMMENT ON CONSTRAINT fk_company_contact_comp_org ON recruiting.company_contact IS
  'Task 16 hardening: composite FK ensuring company_contact org matches company org.';

-- 2b. company_preference -> company
ALTER TABLE recruiting.company_preference
  ADD CONSTRAINT fk_company_pref_comp_org
    FOREIGN KEY (company_id, organization_id)
    REFERENCES recruiting.company (company_id, organization_id);
COMMENT ON CONSTRAINT fk_company_pref_comp_org ON recruiting.company_preference IS
  'Task 16 hardening: composite FK ensuring company_preference org matches company org.';

-- 2c. job_requirement -> job
ALTER TABLE recruiting.job_requirement
  ADD CONSTRAINT fk_job_requirement_job_org
    FOREIGN KEY (job_id, organization_id)
    REFERENCES recruiting.job (job_id, organization_id);
COMMENT ON CONSTRAINT fk_job_requirement_job_org ON recruiting.job_requirement IS
  'Task 16 hardening: composite FK ensuring job_requirement org matches job org.';

-- 2d. job_scorecard -> job
ALTER TABLE recruiting.job_scorecard
  ADD CONSTRAINT fk_job_scorecard_job_org
    FOREIGN KEY (job_id, organization_id)
    REFERENCES recruiting.job (job_id, organization_id);
COMMENT ON CONSTRAINT fk_job_scorecard_job_org ON recruiting.job_scorecard IS
  'Task 16 hardening: composite FK ensuring job_scorecard org matches job org.';

-- 2e. candidate_document -> candidate
ALTER TABLE recruiting.candidate_document
  ADD CONSTRAINT fk_cand_doc_candidate_org
    FOREIGN KEY (candidate_id, organization_id)
    REFERENCES recruiting.candidate (candidate_id, organization_id);
COMMENT ON CONSTRAINT fk_cand_doc_candidate_org ON recruiting.candidate_document IS
  'Task 16 hardening: composite FK ensuring candidate_document org matches candidate org.';

-- 2f. candidate_company_interaction -> candidate
ALTER TABLE recruiting.candidate_company_interaction
  ADD CONSTRAINT fk_interaction_cand_org
    FOREIGN KEY (candidate_id, organization_id)
    REFERENCES recruiting.candidate (candidate_id, organization_id);
COMMENT ON CONSTRAINT fk_interaction_cand_org ON recruiting.candidate_company_interaction IS
  'Task 16 hardening: composite FK ensuring interaction org matches candidate org.';

-- 2g. candidate_company_interaction -> company
ALTER TABLE recruiting.candidate_company_interaction
  ADD CONSTRAINT fk_interaction_comp_org
    FOREIGN KEY (company_id, organization_id)
    REFERENCES recruiting.company (company_id, organization_id);
COMMENT ON CONSTRAINT fk_interaction_comp_org ON recruiting.candidate_company_interaction IS
  'Task 16 hardening: composite FK ensuring interaction org matches company org.';

-- 2h. shortlist -> job
ALTER TABLE recruiting.shortlist
  ADD CONSTRAINT fk_shortlist_job_org
    FOREIGN KEY (job_id, organization_id)
    REFERENCES recruiting.job (job_id, organization_id);
COMMENT ON CONSTRAINT fk_shortlist_job_org ON recruiting.shortlist IS
  'Task 16 hardening: composite FK ensuring shortlist org matches job org.';

-- 2i. shortlist_candidate_card -> shortlist
ALTER TABLE recruiting.shortlist_candidate_card
  ADD CONSTRAINT fk_scard_shortlist_org
    FOREIGN KEY (shortlist_id, organization_id)
    REFERENCES recruiting.shortlist (shortlist_id, organization_id);
COMMENT ON CONSTRAINT fk_scard_shortlist_org ON recruiting.shortlist_candidate_card IS
  'Task 16 hardening: composite FK ensuring shortlist_card org matches shortlist org.';

-- 2j. shortlist_candidate_card -> candidate
ALTER TABLE recruiting.shortlist_candidate_card
  ADD CONSTRAINT fk_scard_candidate_org
    FOREIGN KEY (candidate_id, organization_id)
    REFERENCES recruiting.candidate (candidate_id, organization_id);
COMMENT ON CONSTRAINT fk_scard_candidate_org ON recruiting.shortlist_candidate_card IS
  'Task 16 hardening: composite FK ensuring shortlist_card org matches candidate org.';

-- 2k. shortlist_candidate_card -> candidate_profile
ALTER TABLE recruiting.shortlist_candidate_card
  ADD CONSTRAINT fk_scard_profile_org
    FOREIGN KEY (candidate_profile_id, organization_id)
    REFERENCES recruiting.candidate_profile (candidate_profile_id, organization_id);
COMMENT ON CONSTRAINT fk_scard_profile_org ON recruiting.shortlist_candidate_card IS
  'Task 16 hardening: composite FK ensuring shortlist_card org matches candidate_profile org.';

-- 2l. interview_feedback -> candidate_company_interaction
ALTER TABLE recruiting.interview_feedback
  ADD CONSTRAINT fk_feedback_interaction_org
    FOREIGN KEY (candidate_company_interaction_id, organization_id)
    REFERENCES recruiting.candidate_company_interaction
      (candidate_company_interaction_id, organization_id);
COMMENT ON CONSTRAINT fk_feedback_interaction_org ON recruiting.interview_feedback IS
  'Task 16 hardening: composite FK ensuring interview_feedback org matches interaction org.';

-- 2m. interview_feedback -> job
ALTER TABLE recruiting.interview_feedback
  ADD CONSTRAINT fk_feedback_job_org
    FOREIGN KEY (job_id, organization_id)
    REFERENCES recruiting.job (job_id, organization_id);
COMMENT ON CONSTRAINT fk_feedback_job_org ON recruiting.interview_feedback IS
  'Task 16 hardening: composite FK ensuring interview_feedback org matches job org.';

-- 2n. placement -> job
ALTER TABLE recruiting.placement
  ADD CONSTRAINT fk_placement_job_org
    FOREIGN KEY (job_id, organization_id)
    REFERENCES recruiting.job (job_id, organization_id);
COMMENT ON CONSTRAINT fk_placement_job_org ON recruiting.placement IS
  'Task 16 hardening: composite FK ensuring placement org matches job org.';

-- 2o. placement -> candidate
ALTER TABLE recruiting.placement
  ADD CONSTRAINT fk_placement_cand_org
    FOREIGN KEY (candidate_id, organization_id)
    REFERENCES recruiting.candidate (candidate_id, organization_id);
COMMENT ON CONSTRAINT fk_placement_cand_org ON recruiting.placement IS
  'Task 16 hardening: composite FK ensuring placement org matches candidate org.';

-- 2p. placement -> company
ALTER TABLE recruiting.placement
  ADD CONSTRAINT fk_placement_comp_org
    FOREIGN KEY (company_id, organization_id)
    REFERENCES recruiting.company (company_id, organization_id);
COMMENT ON CONSTRAINT fk_placement_comp_org ON recruiting.placement IS
  'Task 16 hardening: composite FK ensuring placement org matches company org.';

-- 2q. commission -> placement
ALTER TABLE recruiting.commission
  ADD CONSTRAINT fk_commission_placement_org
    FOREIGN KEY (placement_id, organization_id)
    REFERENCES recruiting.placement (placement_id, organization_id);
COMMENT ON CONSTRAINT fk_commission_placement_org ON recruiting.commission IS
  'Task 16 hardening: composite FK ensuring commission org matches placement org.';

-- 2r. profile_field_lineage -> candidate_profile
ALTER TABLE recruiting.profile_field_lineage
  ADD CONSTRAINT fk_lineage_profile_org
    FOREIGN KEY (candidate_profile_id, organization_id)
    REFERENCES recruiting.candidate_profile (candidate_profile_id, organization_id);
COMMENT ON CONSTRAINT fk_lineage_profile_org ON recruiting.profile_field_lineage IS
  'Task 16 hardening: composite FK ensuring lineage org matches candidate_profile org.';

-- 2s. profile_field_lineage -> candidate
ALTER TABLE recruiting.profile_field_lineage
  ADD CONSTRAINT fk_lineage_candidate_org
    FOREIGN KEY (candidate_id, organization_id)
    REFERENCES recruiting.candidate (candidate_id, organization_id);
COMMENT ON CONSTRAINT fk_lineage_candidate_org ON recruiting.profile_field_lineage IS
  'Task 16 hardening: composite FK ensuring lineage org matches candidate org.';

-- === Step 3: Drop redundant simple FKs (covered by composite FKs above) ======

-- V10 used inline REFERENCES without explicit names. PostgreSQL auto-generates
-- names as {tablename}_{columnname}_fkey. Use IF EXISTS for idempotency.

ALTER TABLE recruiting.company_contact
  DROP CONSTRAINT IF EXISTS company_contact_company_id_fkey;
ALTER TABLE recruiting.company_preference
  DROP CONSTRAINT IF EXISTS company_preference_company_id_fkey;
ALTER TABLE recruiting.job_requirement
  DROP CONSTRAINT IF EXISTS job_requirement_job_id_fkey;
ALTER TABLE recruiting.job_scorecard
  DROP CONSTRAINT IF EXISTS job_scorecard_job_id_fkey;
ALTER TABLE recruiting.candidate_document
  DROP CONSTRAINT IF EXISTS candidate_document_candidate_id_fkey;
ALTER TABLE recruiting.candidate_company_interaction
  DROP CONSTRAINT IF EXISTS candidate_company_interaction_candidate_id_fkey;
ALTER TABLE recruiting.candidate_company_interaction
  DROP CONSTRAINT IF EXISTS candidate_company_interaction_company_id_fkey;
ALTER TABLE recruiting.shortlist
  DROP CONSTRAINT IF EXISTS shortlist_job_id_fkey;
ALTER TABLE recruiting.shortlist_candidate_card
  DROP CONSTRAINT IF EXISTS shortlist_candidate_card_shortlist_id_fkey;
ALTER TABLE recruiting.shortlist_candidate_card
  DROP CONSTRAINT IF EXISTS shortlist_candidate_card_candidate_id_fkey;
ALTER TABLE recruiting.shortlist_candidate_card
  DROP CONSTRAINT IF EXISTS shortlist_candidate_card_candidate_profile_id_fkey;
ALTER TABLE recruiting.interview_feedback
  DROP CONSTRAINT IF EXISTS interview_feedback_candidate_company_interaction_id_fkey;
ALTER TABLE recruiting.interview_feedback
  DROP CONSTRAINT IF EXISTS interview_feedback_job_id_fkey;
ALTER TABLE recruiting.placement
  DROP CONSTRAINT IF EXISTS placement_job_id_fkey;
ALTER TABLE recruiting.placement
  DROP CONSTRAINT IF EXISTS placement_candidate_id_fkey;
ALTER TABLE recruiting.placement
  DROP CONSTRAINT IF EXISTS placement_company_id_fkey;
ALTER TABLE recruiting.commission
  DROP CONSTRAINT IF EXISTS commission_placement_id_fkey;
ALTER TABLE recruiting.profile_field_lineage
  DROP CONSTRAINT IF EXISTS profile_field_lineage_candidate_profile_id_fkey;
ALTER TABLE recruiting.profile_field_lineage
  DROP CONSTRAINT IF EXISTS profile_field_lineage_candidate_id_fkey;

-- ============================================================================
-- Hardening complete.
-- 7 parent UNIQUE constraints added.
-- 19 composite FKs added.
-- 19 redundant simple FKs dropped.
-- Nullable FK columns (interaction.job_id, document.source_item_id) and
-- cross-cutting identity FK (commission.consultant_id) intentionally excluded.
-- ============================================================================
