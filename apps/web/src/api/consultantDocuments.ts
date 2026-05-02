import { ApiResult, apiRequest } from "./http";

export type ConsultantDocumentUpload = {
  sourceItemId: string;
  informationPacketId: string | null;
  scanStatus: string;
};

export type ConsultantParsedDocument = {
  processingStatus: string;
  parserName: string;
  parserVersion: string;
  mediaType: string;
  ocrRequired: boolean;
  chunkCount: number;
  createdAt: string;
  completedAt: string | null;
  failureReason: string | null;
};

export async function uploadConsultantDocument(formData: FormData): Promise<ApiResult<ConsultantDocumentUpload>> {
  return apiRequest<ConsultantDocumentUpload>("/api/consultant/documents/upload", {
    method: "POST",
    body: formData,
  });
}

export function parseConsultantDocument(sourceItemId: string): Promise<ApiResult<ConsultantParsedDocument>> {
  return apiRequest<ConsultantParsedDocument>(`/api/consultant/documents/${encodeURIComponent(sourceItemId)}/parse`, {
    method: "POST",
  });
}
