import { loadAccessToken } from "../auth/accessTokenStorage";

export type ApiError = {
  errorCode: string;
  safeReason: string;
  safeMessage: string;
};

export type ApiEnvelope<T> =
  | { data: T; error: null }
  | { data: null; error: ApiError };

export type ApiResult<T> =
  | { status: "ready"; data: T }
  | { status: "unauthenticated" | "denied" | "invalid_request" | "unavailable" | "failed"; error?: string };

type ErrorStatus = Exclude<ApiResult<never>["status"], "ready">;

function mapStatus(status: number): ErrorStatus {
  if (status === 400) return "invalid_request";
  if (status === 401) return "unauthenticated";
  if (status === 403) return "denied";
  if (status === 404 || status === 503) return "unavailable";
  return "failed";
}

export async function apiRequest<T>(
  input: string,
  init?: RequestInit,
): Promise<ApiResult<T>> {
  try {
    const accessToken = loadAccessToken();
    const response = await fetch(input, {
      ...init,
      headers: {
        ...(init?.headers ?? {}),
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
    });

    const envelope = await parseEnvelope<T>(response);
    if (!response.ok) {
      return {
        status: mapStatus(response.status),
        error: envelope?.error?.safeMessage ?? envelope?.error?.safeReason,
      };
    }
    if (!envelope) {
      return {
        status: "failed",
        error: `Unexpected empty response from ${input}.`,
      };
    }
    if (!envelope.data) {
      return {
        status: envelope.error?.errorCode === "access_denied" ? "denied" : "failed",
        error: envelope.error?.safeMessage,
      };
    }
    return { status: "ready", data: envelope.data };
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      return { status: "failed", error: "The request was cancelled." };
    }
    return { status: "unavailable", error: "The backend is unavailable." };
  }
}

async function parseEnvelope<T>(response: Response): Promise<ApiEnvelope<T> | null> {
  const text = await response.text();
  if (!text.trim()) {
    return null;
  }
  try {
    return JSON.parse(text) as ApiEnvelope<T>;
  } catch {
    return null;
  }
}

export function asJson(body: unknown): RequestInit {
  return asMethodJson("POST", body);
}

export function asMethodJson(method: "POST" | "PUT", body: unknown): RequestInit {
  return {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  };
}

export type PagedResult<T> = {
  items: T[];
  totalCount: number;
  limit: number;
  offset: number;
  hasMore: boolean;
};
