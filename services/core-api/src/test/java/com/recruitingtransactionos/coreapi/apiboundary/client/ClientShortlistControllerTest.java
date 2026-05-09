package com.recruitingtransactionos.coreapi.apiboundary.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientShortlistControllerTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000004201");
  private static final UUID CLIENT_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000004202");

  @Mock private ClientApiQueryService queryService;
  @Mock private ClientApiCommandService commandService;

  @Test
  void listShortlistsReturnsEmptyPageWhenNoClientVisibleShortlistsExist() {
    when(queryService.listShortlists(any(), any(), any())).thenReturn(List.of());
    ClientShortlistController controller = new ClientShortlistController(queryService, commandService);

    var response = controller.listShortlists(principal());

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    ApiSafeResponseBody data = response.getBody().data();
    assertThat(data).isInstanceOfSatisfying(PagedResult.class, page -> {
      assertThat(page.items()).isEmpty();
      assertThat(page.totalCount()).isZero();
      assertThat(page.limit()).isGreaterThan(0);
      assertThat(page.offset()).isZero();
    });
  }

  private static RtoAuthenticatedPrincipal principal() {
    return new RtoAuthenticatedPrincipal(
        CLIENT_ACTOR_ID,
        ORGANIZATION_ID,
        PortalRole.CLIENT,
        "Pilot Client",
        UUID.fromString("00000000-0000-0000-0000-000000004203"));
  }
}
