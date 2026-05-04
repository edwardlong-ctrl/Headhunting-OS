import { type ApiResult, apiRequest } from "./http";

export type ClientDisclosedCandidateField = {
  fieldPath: string;
  jsonValue: string;
};

export type ClientDisclosedCandidate = {
  shortlistId: string;
  shortlistCandidateCardId: string;
  disclosureRecordRef: string;
  candidateId: string;
  candidateProfileId: string;
  candidateStatus: string;
  profileVersion: string;
  disclosedFields: ClientDisclosedCandidateField[];
};

export function fetchClientDisclosedCandidate(
  shortlistId: string,
  shortlistCandidateCardId: string,
): Promise<ApiResult<ClientDisclosedCandidate>> {
  return apiRequest<ClientDisclosedCandidate>(
    `/api/client/disclosed-candidates/${encodeURIComponent(shortlistId)}/${encodeURIComponent(shortlistCandidateCardId)}`,
    undefined,
    "client",
  );
}
