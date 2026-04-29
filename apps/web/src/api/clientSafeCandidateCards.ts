export type ClientSafeCandidateCard = {
  anonymousCardRef: string;
  anonymousCandidateRef: string;
  projectionVersion: string;
  redactionLevel: string;
  generalizedHeadline: string;
  generalizedRoleFamily: string;
  generalizedSeniorityBand: string;
  generalizedLocationRegion: string;
  safeSummary: string;
  safeSkillSummary: string;
  safeEvidenceSummaries: string[];
  safeMatchNarratives: string[];
};

type ApiSafeError = {
  errorCode: string;
  safeReason: string;
  safeMessage: string;
};

type ApiResponseEnvelope<T> =
  | {
      data: T;
      error: null;
    }
  | {
      data: null;
      error: ApiSafeError;
    };

const CLIENT_PORTAL_ORGANIZATION_ID = "00000000-0000-0000-0000-00000013b001";

export type ClientSafeCandidateCardResult =
  | {
      status: "ready";
      card: ClientSafeCandidateCard;
    }
  | {
      status: "invalid_ref" | "unavailable" | "denied" | "failed";
    };

const rawUuidPattern =
  /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

export function isAnonymousCardRef(value: string): boolean {
  const normalized = value.trim();
  return normalized.startsWith("card_") && !rawUuidPattern.test(normalized);
}

export async function fetchClientSafeCandidateCard(
  anonymousCardRef: string,
  signal?: AbortSignal,
): Promise<ClientSafeCandidateCardResult> {
  const normalizedCardRef = anonymousCardRef.trim();
  if (!isAnonymousCardRef(normalizedCardRef)) {
    return { status: "invalid_ref" };
  }

  try {
    const response = await fetch(
      `/api/client-safe/candidate-cards/${encodeURIComponent(normalizedCardRef)}`,
      {
        headers: {
          "X-RTO-Actor-Role": "client",
          "X-RTO-Field-Classification": "client_safe",
          "X-RTO-Identity-Disclosure-Requested": "false",
          "X-RTO-Organization-Id": CLIENT_PORTAL_ORGANIZATION_ID,
        },
        signal,
      },
    );

    if (response.status === 400) {
      return { status: "invalid_ref" };
    }

    if (response.status === 403) {
      return { status: "denied" };
    }

    if (response.status === 404 || response.status === 503) {
      return { status: "unavailable" };
    }

    if (!response.ok) {
      return { status: "failed" };
    }

    const envelope = (await response.json()) as ApiResponseEnvelope<ClientSafeCandidateCard>;
    if (!envelope.data) {
      return envelope.error?.errorCode === "access_denied"
        ? { status: "denied" }
        : { status: "unavailable" };
    }

    return { status: "ready", card: envelope.data };
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      return { status: "failed" };
    }

    return { status: "unavailable" };
  }
}
