package com.recruitingtransactionos.coreapi.datalifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.ConflictResolutionCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.ConflictResolutionResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.ConflictResolutionStatus;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleEntitySnapshot;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleEntityType;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleFieldSnapshot;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleFieldStatus;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDecision;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDetectionCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDetectionResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.MergeProposalCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.MergeProposalResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.MergeProposalStatus;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.RetentionDeletionCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.RetentionDeletionResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.RetentionDeletionStatus;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.StaleDetectionCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.StaleDetectionResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.StaleFieldDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataLifecycleServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000460001");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000460002");
  private static final UUID CANDIDATE_A = UUID.fromString("00000000-0000-0000-0000-000000460101");
  private static final UUID CANDIDATE_B = UUID.fromString("00000000-0000-0000-0000-000000460102");
  private static final UUID COMPANY_A = UUID.fromString("00000000-0000-0000-0000-000000460201");
  private static final UUID JOB_A = UUID.fromString("00000000-0000-0000-0000-000000460301");
  private static final UUID JOB_B = UUID.fromString("00000000-0000-0000-0000-000000460302");
  private static final Instant NOW = Instant.parse("2026-05-09T10:00:00Z");

  private final RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
  private final DataLifecycleService service = new DataLifecycleService(
      new WorkflowEventService(workflowEvents));

  @Test
  void highConfidenceCandidateDuplicateBlocksByIdentityFingerprintAndAuditsDecision() {
    DataLifecycleEntitySnapshot incoming = entity(
        DataLifecycleEntityType.CANDIDATE,
        CANDIDATE_A,
        Map.of("identityFingerprintHash", "fingerprint:alice"));
    DataLifecycleEntitySnapshot existing = entity(
        DataLifecycleEntityType.CANDIDATE,
        CANDIDATE_B,
        Map.of("identityFingerprintHash", "fingerprint:alice"));

    DuplicateDetectionResult result = service.evaluateDuplicates(new DuplicateDetectionCommand(
        ORG_ID,
        incoming,
        List.of(existing),
        actor(ActorRole.CONSULTANT),
        NOW,
        "candidate intake duplicate check"));

    assertThat(result.decision()).isEqualTo(DuplicateDecision.HIGH_CONFIDENCE_BLOCK);
    assertThat(result.match().orElseThrow().matchedEntityId()).isEqualTo(CANDIDATE_B);
    assertThat(result.match().orElseThrow().justification())
        .contains("identity fingerprint");
    assertThat(workflowEvents.onlyCommand().action()).isEqualTo("DATA_DUPLICATE_BLOCKED");
    assertThat(workflowEvents.onlyCommand().entity().entityType()).isEqualTo("CANDIDATE");
  }

  @Test
  void lowConfidenceCompanyDuplicateWarnsWithJustificationInsteadOfBlocking() {
    DataLifecycleEntitySnapshot incoming = entity(
        DataLifecycleEntityType.COMPANY,
        COMPANY_A,
        Map.of("name", "OpenAI China", "website", "https://openai.example/cn"));
    DataLifecycleEntitySnapshot existing = entity(
        DataLifecycleEntityType.COMPANY,
        UUID.fromString("00000000-0000-0000-0000-000000460202"),
        Map.of("name", "OpenAI China", "website", "https://different.example"));

    DuplicateDetectionResult result = service.evaluateDuplicates(new DuplicateDetectionCommand(
        ORG_ID,
        incoming,
        List.of(existing),
        actor(ActorRole.CONSULTANT),
        NOW,
        "company duplicate screen"));

    assertThat(result.decision()).isEqualTo(DuplicateDecision.LOW_CONFIDENCE_WARNING);
    assertThat(result.match().orElseThrow().justification())
        .contains("normalized company name");
    assertThat(workflowEvents.onlyCommand().action()).isEqualTo("DATA_DUPLICATE_WARNING_RECORDED");
  }

  @Test
  void blankCompanyIdentifiersDoNotCreateLowConfidenceDuplicateWarnings() {
    DataLifecycleEntitySnapshot incoming = entity(
        DataLifecycleEntityType.COMPANY,
        COMPANY_A,
        Map.of("name", " ", "website", " "));
    DataLifecycleEntitySnapshot existing = entity(
        DataLifecycleEntityType.COMPANY,
        UUID.fromString("00000000-0000-0000-0000-000000460202"),
        Map.of("name", " ", "website", " "));

    DuplicateDetectionResult result = service.evaluateDuplicates(new DuplicateDetectionCommand(
        ORG_ID,
        incoming,
        List.of(existing),
        actor(ActorRole.CONSULTANT),
        NOW,
        "company duplicate screen"));

    assertThat(result.decision()).isEqualTo(DuplicateDecision.NO_DUPLICATE);
    assertThat(workflowEvents.commands()).isEmpty();
  }

  @Test
  void blankJobDuplicateKeysDoNotCreateLowConfidenceDuplicateWarnings() {
    DataLifecycleEntitySnapshot incoming = entity(
        DataLifecycleEntityType.JOB,
        JOB_A,
        Map.of("companyId", " ", "title", " "));
    DataLifecycleEntitySnapshot existing = entity(
        DataLifecycleEntityType.JOB,
        JOB_B,
        Map.of("companyId", " ", "title", " "));

    DuplicateDetectionResult result = service.evaluateDuplicates(new DuplicateDetectionCommand(
        ORG_ID,
        incoming,
        List.of(existing),
        actor(ActorRole.CONSULTANT),
        NOW,
        "job duplicate screen"));

    assertThat(result.decision()).isEqualTo(DuplicateDecision.NO_DUPLICATE);
    assertThat(workflowEvents.commands()).isEmpty();
  }

  @Test
  void jobTitleMatchWithoutCompanyIdentifierDoesNotCreateDuplicateWarning() {
    DataLifecycleEntitySnapshot incoming = entity(
        DataLifecycleEntityType.JOB,
        JOB_A,
        Map.of("companyId", " ", "title", "Senior DV Engineer"));
    DataLifecycleEntitySnapshot existing = entity(
        DataLifecycleEntityType.JOB,
        JOB_B,
        Map.of("companyId", " ", "title", "senior dv engineer"));

    DuplicateDetectionResult result = service.evaluateDuplicates(new DuplicateDetectionCommand(
        ORG_ID,
        incoming,
        List.of(existing),
        actor(ActorRole.CONSULTANT),
        NOW,
        "job duplicate screen"));

    assertThat(result.decision()).isEqualTo(DuplicateDecision.NO_DUPLICATE);
    assertThat(workflowEvents.commands()).isEmpty();
  }

  @Test
  void lowConfidenceJobDuplicateWarnsWhenSameCompanyAndNormalizedTitleMatch() {
    DataLifecycleEntitySnapshot incoming = entity(
        DataLifecycleEntityType.JOB,
        JOB_A,
        Map.of("companyId", COMPANY_A.toString(), "title", "Senior DV Engineer"));
    DataLifecycleEntitySnapshot existing = entity(
        DataLifecycleEntityType.JOB,
        JOB_B,
        Map.of("companyId", COMPANY_A.toString(), "title", "senior dv engineer"));

    DuplicateDetectionResult result = service.evaluateDuplicates(new DuplicateDetectionCommand(
        ORG_ID,
        incoming,
        List.of(existing),
        actor(ActorRole.CONSULTANT),
        NOW,
        "job duplicate screen"));

    assertThat(result.decision()).isEqualTo(DuplicateDecision.LOW_CONFIDENCE_WARNING);
    assertThat(result.match().orElseThrow().justification())
        .contains("same company and normalized job title");
  }

  @Test
  void mergeProposalBlocksConfirmedFactOverwriteAndAuditsConflict() {
    DataLifecycleEntitySnapshot source = entity(
        DataLifecycleEntityType.CANDIDATE,
        CANDIDATE_A,
        Map.of(),
        List.of(new DataLifecycleFieldSnapshot(
            "profile.headline",
            "AI Infrastructure Lead",
            DataLifecycleFieldStatus.HUMAN_ACKNOWLEDGED)));
    DataLifecycleEntitySnapshot target = entity(
        DataLifecycleEntityType.CANDIDATE,
        CANDIDATE_B,
        Map.of(),
        List.of(new DataLifecycleFieldSnapshot(
            "profile.headline",
            "Compiler Verification Lead",
            DataLifecycleFieldStatus.CANDIDATE_CONFIRMED)));

    MergeProposalResult result = service.proposeMerge(new MergeProposalCommand(
        ORG_ID,
        source,
        target,
        actor(ActorRole.CONSULTANT),
        NOW,
        "candidate duplicate merge review"));

    assertThat(result.status())
        .isEqualTo(MergeProposalStatus.BLOCKED_CONFIRMED_FACT_CONFLICT);
    assertThat(result.conflicts()).singleElement().satisfies(conflict -> {
      assertThat(conflict.fieldPath()).isEqualTo("profile.headline");
      assertThat(conflict.confirmedTargetStatus())
          .isEqualTo(DataLifecycleFieldStatus.CANDIDATE_CONFIRMED);
    });
    assertThat(workflowEvents.onlyCommand().action())
        .isEqualTo("DATA_MERGE_BLOCKED_CONFIRMED_FACT_CONFLICT");
    assertThat(workflowEvents.onlyCommand().afterState().json())
        .contains("confirmed_fact_conflict");
  }

  @Test
  void staleDetectionSchedulesRefreshWorkflowWithoutMutatingCanonicalFields() {
    DataLifecycleEntitySnapshot candidate = entity(
        DataLifecycleEntityType.CANDIDATE,
        CANDIDATE_A,
        Map.of(),
        List.of(
            new DataLifecycleFieldSnapshot(
                "availability.notice_period",
                "30 days",
                DataLifecycleFieldStatus.HUMAN_ACKNOWLEDGED,
                Instant.parse("2026-01-01T00:00:00Z")),
            new DataLifecycleFieldSnapshot(
                "profile.headline",
                "CPU DV lead",
                DataLifecycleFieldStatus.EXTERNAL_VERIFIED,
                Instant.parse("2025-01-01T00:00:00Z"))));

    StaleDetectionResult result = service.detectStaleFields(new StaleDetectionCommand(
        ORG_ID,
        candidate,
        NOW.minusSeconds(60L * 60 * 24 * 60),
        NOW.plusSeconds(60L * 60 * 24 * 7),
        actor(ActorRole.CONSULTANT),
        NOW,
        "refresh stale candidate operating fields"));

    assertThat(result.staleFields()).extracting(StaleFieldDecision::fieldPath)
        .containsExactly("availability.notice_period");
    assertThat(result.refreshWorkflowRequired()).isTrue();
    assertThat(workflowEvents.onlyCommand().action()).isEqualTo("DATA_REFRESH_REQUESTED");
  }

  @Test
  void auditStatesRemainValidJsonWhenValuesContainQuotes() throws Exception {
    ConflictResolutionResult result = service.recordConflictResolution(
        new ConflictResolutionCommand(
            ORG_ID,
            entity(
                DataLifecycleEntityType.CANDIDATE,
                CANDIDATE_A,
                Map.of(),
                List.of(new DataLifecycleFieldSnapshot(
                    "compensation.expected_salary",
                    "900k CNY",
                    DataLifecycleFieldStatus.CONFLICTING))),
            "compensation.expected_salary",
            "accepted candidate-confirmed \"900k CNY\" after source review",
            actor(ActorRole.CONSULTANT),
            NOW,
            "resolve salary expectation conflict"));

    assertThat(result.status()).isEqualTo(ConflictResolutionStatus.RECORDED_FOR_REVIEW);
    JsonNode afterState = OBJECT_MAPPER.readTree(workflowEvents.onlyCommand().afterState().json());
    assertThat(afterState.get("resolution").asText())
        .isEqualTo("accepted candidate-confirmed \"900k CNY\" after source review");
  }

  @Test
  void conflictResolutionWorkflowIsAuditedBeforeAnyCanonicalMutation() {
    ConflictResolutionResult result = service.recordConflictResolution(
        new ConflictResolutionCommand(
            ORG_ID,
            entity(
                DataLifecycleEntityType.CANDIDATE,
                CANDIDATE_A,
                Map.of(),
                List.of(new DataLifecycleFieldSnapshot(
                    "compensation.expected_salary",
                    "900k CNY",
                    DataLifecycleFieldStatus.CONFLICTING))),
            "compensation.expected_salary",
            "accepted candidate-confirmed compensation expectation after source review",
            actor(ActorRole.CONSULTANT),
            NOW,
            "resolve salary expectation conflict"));

    assertThat(result.status()).isEqualTo(ConflictResolutionStatus.RECORDED_FOR_REVIEW);
    assertThat(result.canonicalMutationPerformed()).isFalse();
    assertThat(workflowEvents.onlyCommand().action())
        .isEqualTo("DATA_CONFLICT_RESOLUTION_RECORDED");
    assertThat(workflowEvents.onlyCommand().afterState().json())
        .contains("compensation.expected_salary")
        .contains("canonicalMutationPerformed\":false");
  }

  @Test
  void retentionDeletionExecutionRequiresTombstoneWhenConfirmedFactsExist() {
    DataLifecycleEntitySnapshot candidate = entity(
        DataLifecycleEntityType.CANDIDATE,
        CANDIDATE_A,
        Map.of("status", "archived"),
        List.of(new DataLifecycleFieldSnapshot(
            "profile.headline",
            "GPU compiler lead",
            DataLifecycleFieldStatus.EXTERNAL_VERIFIED)));

    RetentionDeletionResult blocked = service.executeRetentionDeletion(
        new RetentionDeletionCommand(
            ORG_ID,
            candidate,
            false,
            actor(ActorRole.ADMIN),
            NOW,
            "retention deletion review"));

    assertThat(blocked.status()).isEqualTo(RetentionDeletionStatus.BLOCKED_CONFIRMED_FACTS);
    assertThat(workflowEvents.onlyCommand().action()).isEqualTo("DATA_RETENTION_DELETION_BLOCKED");

    RetentionDeletionResult executed = service.executeRetentionDeletion(
        new RetentionDeletionCommand(
            ORG_ID,
            candidate,
            true,
            actor(ActorRole.ADMIN),
            NOW.plusSeconds(1),
            "retention deletion with audit tombstone"));

    assertThat(executed.status()).isEqualTo(RetentionDeletionStatus.SOFT_DELETE_EXECUTED);
    assertThat(workflowEvents.commands()).extracting(WorkflowEventAppendCommand::action)
        .containsExactly("DATA_RETENTION_DELETION_BLOCKED", "DATA_RETENTION_DELETION_EXECUTED");
  }

  private static DataLifecycleEntitySnapshot entity(
      DataLifecycleEntityType type,
      UUID entityId,
      Map<String, String> attributes) {
    return entity(type, entityId, attributes, List.of());
  }

  private static DataLifecycleEntitySnapshot entity(
      DataLifecycleEntityType type,
      UUID entityId,
      Map<String, String> attributes,
      List<DataLifecycleFieldSnapshot> fields) {
    return new DataLifecycleEntitySnapshot(
        ORG_ID,
        type,
        entityId,
        1,
        attributes,
        fields,
        NOW);
  }

  private static ActorRef actor(ActorRole role) {
    return new ActorRef(ACTOR_ID, role);
  }

  private static final class RecordingWorkflowEventPort implements WorkflowEventPort {

    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();

    @Override
    public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey idempotencyKey) {
      return Optional.empty();
    }

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      return new WorkflowEventAppendResult(new WorkflowEventId(UUID.randomUUID()));
    }

    private WorkflowEventAppendCommand onlyCommand() {
      assertThat(commands).hasSize(1);
      return commands.get(0);
    }

    private List<WorkflowEventAppendCommand> commands() {
      return List.copyOf(commands);
    }
  }
}
