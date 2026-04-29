ALTER TABLE identity.user_account
  ADD CONSTRAINT user_account_id_org_uidx UNIQUE (user_account_id, organization_id);

ALTER TABLE privacy.consent_record
  ADD CONSTRAINT consent_record_org_core_scope_uidx UNIQUE (
    organization_id,
    consent_record_ref,
    candidate_ref,
    candidate_profile_ref,
    job_ref
  );

ALTER TABLE privacy.unlock_decision
  ADD CONSTRAINT unlock_decision_approver_org_fk
    FOREIGN KEY (approved_by_user_id, organization_id)
    REFERENCES identity.user_account (user_account_id, organization_id)
    NOT VALID,
  ADD CONSTRAINT unlock_decision_org_full_scope_uidx UNIQUE (
    organization_id,
    unlock_decision_ref,
    candidate_ref,
    candidate_profile_ref,
    job_ref,
    client_ref
  );

ALTER TABLE privacy.disclosure_record
  ADD CONSTRAINT disclosure_record_consent_org_scope_fk
    FOREIGN KEY (
      organization_id,
      consent_record_ref,
      candidate_ref,
      candidate_profile_ref,
      job_ref
    )
    REFERENCES privacy.consent_record (
      organization_id,
      consent_record_ref,
      candidate_ref,
      candidate_profile_ref,
      job_ref
    )
    NOT VALID,
  ADD CONSTRAINT disclosure_record_unlock_org_scope_fk
    FOREIGN KEY (
      organization_id,
      unlock_decision_ref,
      candidate_ref,
      candidate_profile_ref,
      job_ref,
      client_ref
    )
    REFERENCES privacy.unlock_decision (
      organization_id,
      unlock_decision_ref,
      candidate_ref,
      candidate_profile_ref,
      job_ref,
      client_ref
    )
    NOT VALID;
