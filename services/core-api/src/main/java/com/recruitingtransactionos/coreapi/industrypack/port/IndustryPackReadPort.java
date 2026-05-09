package com.recruitingtransactionos.coreapi.industrypack.port;

import com.recruitingtransactionos.coreapi.industrypack.IndustryPack;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackCalibrationProfile;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackId;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackKey;
import com.recruitingtransactionos.coreapi.industrypack.IndustryRoleFamilyTemplate;
import com.recruitingtransactionos.coreapi.industrypack.OntologyVersion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IndustryPackReadPort {
  Optional<IndustryPack> findById(IndustryPackId industryPackId);
  Optional<IndustryPack> findByKey(IndustryPackKey packKey);
  Optional<OntologyVersion> findActiveOntologyVersion(IndustryPackId industryPackId, Instant asOf);
  Optional<IndustryRoleFamilyTemplate> findRoleFamilyTemplate(
      IndustryPackId industryPackId,
      java.util.UUID ontologyVersionId,
      String roleFamily);

  default List<IndustryPackCalibrationProfile> findCalibrationProfiles(Instant asOf) {
    return List.of();
  }
}
