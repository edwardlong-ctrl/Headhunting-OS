package com.recruitingtransactionos.coreapi.candidateprofile.port;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateProfilePersistencePort {

  CandidateProfile createCandidateProfile(CandidateProfile candidateProfile);

  Optional<CandidateProfile> findCandidateProfileByIdAndOrganizationId(
      UUID organizationId,
      CandidateProfileId candidateProfileId);

  Optional<CandidateProfile> findCandidateProfileByCandidateIdAndOrganizationId(
      UUID organizationId,
      CandidateId candidateId);

  CandidateProfileField upsertCandidateProfileField(
      UUID organizationId,
      CandidateProfileId candidateProfileId,
      CandidateProfileField field);

  List<CandidateProfileField> listCandidateProfileFields(
      UUID organizationId,
      CandidateProfileId candidateProfileId);
}
