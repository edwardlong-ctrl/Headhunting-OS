package com.recruitingtransactionos.coreapi.privacyredaction.port;

import com.recruitingtransactionos.coreapi.privacyredaction.PersistedReidentificationRiskAssessment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence boundary for Task 30
 * {@link PersistedReidentificationRiskAssessment} records.
 *
 * <p>Implementations must enforce organization scope on every read; the
 * primary key on the row is the composite
 * {@code (organization_id, reidentification_risk_assessment_ref)}.
 */
public interface ReidentificationRiskAssessmentPort {

  /**
   * Persists a new assessment row. Idempotency is the caller's responsibility
   * (the caller should derive a deterministic
   * {@code reidentification_risk_assessment_ref} from the workflow context
   * if it wants idempotent appends).
   */
  PersistedReidentificationRiskAssessment append(
      PersistedReidentificationRiskAssessment assessment);

  /**
   * Looks up by composite key. Returns {@link Optional#empty()} if no row
   * exists with that ref within the organization.
   */
  Optional<PersistedReidentificationRiskAssessment> findByRefAndOrganizationId(
      UUID organizationId,
      String reidentificationRiskAssessmentRef);

  /**
   * Returns the most recent assessments for the given anonymous candidate
   * card, newest first, scoped to the organization. The {@code limit}
   * controls how many rows are returned and must be at least 1.
   */
  List<PersistedReidentificationRiskAssessment> findRecentByCandidateCardId(
      UUID organizationId,
      String candidateCardId,
      int limit);
}
