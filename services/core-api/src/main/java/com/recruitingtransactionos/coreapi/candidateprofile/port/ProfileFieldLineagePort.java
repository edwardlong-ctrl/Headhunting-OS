package com.recruitingtransactionos.coreapi.candidateprofile.port;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceType;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.ProfileFieldLineage;
import java.util.List;
import java.util.UUID;

public interface ProfileFieldLineagePort {

  ProfileFieldLineage append(ProfileFieldLineage lineage);

  List<ProfileFieldLineage> findByProfileAndFieldPath(
      UUID organizationId,
      CandidateProfileId candidateProfileId,
      CandidateProfileFieldPath fieldPath);

  List<ProfileFieldLineage> findByCandidateAndFieldPath(
      UUID organizationId,
      CandidateId candidateId,
      CandidateProfileFieldPath fieldPath);

  List<ProfileFieldLineage> findBySourceTypeAndSourceId(
      UUID organizationId,
      CandidateProfileFieldSourceType sourceType,
      String sourceId);
}
