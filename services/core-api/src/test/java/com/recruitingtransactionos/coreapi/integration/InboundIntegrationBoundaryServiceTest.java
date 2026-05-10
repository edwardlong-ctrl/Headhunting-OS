package com.recruitingtransactionos.coreapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InboundIntegrationBoundaryServiceTest {

  private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-000000490101");
  private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-000000490102");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000490103");

  @Test
  void externalInboundPayloadIsAcceptedForReviewThroughSourceItemAndPacketOnly() {
    RecordingInboundSink sink = new RecordingInboundSink();
    InboundIntegrationBoundaryService service = new InboundIntegrationBoundaryService(sink);

    InboundIntegrationResult result = service.acceptInbound(inboundCommand(
        InboundIntegrationPurpose.SOURCE_INTAKE,
        ORG_A,
        ORG_A));

    assertThat(result.status()).isEqualTo(InboundIntegrationStatus.ACCEPTED_FOR_REVIEW);
    assertThat(result.confirmedFactWritten()).isFalse();
    assertThat(result.sourceItemId()).isNotNull();
    assertThat(result.informationPacketId()).isNotNull();
    assertThat(sink.commands).hasSize(1);
    assertThat(sink.commands.get(0).purpose()).isEqualTo(InboundIntegrationPurpose.SOURCE_INTAKE);
  }

  @Test
  void externalInboundPayloadCannotRequestCanonicalConfirmedFactWrite() {
    RecordingInboundSink sink = new RecordingInboundSink();
    InboundIntegrationBoundaryService service = new InboundIntegrationBoundaryService(sink);

    InboundIntegrationResult result = service.acceptInbound(inboundCommand(
        InboundIntegrationPurpose.CONFIRMED_FACT_WRITE,
        ORG_A,
        ORG_A));

    assertThat(result.status()).isEqualTo(InboundIntegrationStatus.BLOCKED_CONFIRMED_FACT_WRITE);
    assertThat(result.confirmedFactWritten()).isFalse();
    assertThat(sink.commands).isEmpty();
  }

  @Test
  void crossOrgInboundAttemptFailsClosedBeforeIntakeMutation() {
    RecordingInboundSink sink = new RecordingInboundSink();
    InboundIntegrationBoundaryService service = new InboundIntegrationBoundaryService(sink);

    InboundIntegrationResult result = service.acceptInbound(inboundCommand(
        InboundIntegrationPurpose.SOURCE_INTAKE,
        ORG_A,
        ORG_B));

    assertThat(result.status()).isEqualTo(InboundIntegrationStatus.BLOCKED_CROSS_ORG);
    assertThat(result.confirmedFactWritten()).isFalse();
    assertThat(sink.commands).isEmpty();
  }

  private static InboundIntegrationCommand inboundCommand(
      InboundIntegrationPurpose purpose,
      UUID organizationId,
      UUID actorOrganizationId) {
    return new InboundIntegrationCommand(
        organizationId,
        ACTOR_ID,
        actorOrganizationId,
        "mailgun",
        IntegrationChannel.EMAIL,
        "message-4901",
        SourceItemType.EMAIL,
        InformationPacketType.CANDIDATE,
        IntendedEntityType.CANDIDATE,
        "{\"from\":\"candidate@example.com\",\"body\":\"Open to opportunities\"}",
        "{\"integration\":\"email\"}",
        purpose);
  }

  private static final class RecordingInboundSink implements InboundIntegrationSink {
    private final List<InboundIntegrationCommand> commands = new ArrayList<>();

    @Override
    public InboundIntakeReceipt acceptForReview(InboundIntegrationCommand command) {
      commands.add(command);
      return new InboundIntakeReceipt(UUID.randomUUID(), UUID.randomUUID());
    }
  }
}
