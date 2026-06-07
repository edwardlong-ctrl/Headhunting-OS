import type { ConsultantWorkflowEvent } from "../../api/consultantWorkflow";

export const SHORTLIST_BUILDER_INITIAL_STATUS = "draft";

export const SHORTLIST_BUILDER_EDITABLE_STATUSES = [
  "draft",
  "ready_for_review",
] as const;

export const CONSULTANT_WORKFLOW_ENTITY_TYPE_OPTIONS = [
  { value: "CANDIDATE", label: "candidate" },
  { value: "JOB", label: "job" },
  { value: "SHORTLIST", label: "shortlist" },
  { value: "CONSENT", label: "consent" },
  { value: "DISCLOSURE", label: "disclosure" },
  { value: "PLACEMENT", label: "placement" },
  { value: "COMMISSION", label: "commission" },
  { value: "INFORMATION_PACKET", label: "information_packet" },
  { value: "SOURCE_ITEM", label: "source_item" },
] as const;

export type ShortlistPreSendCheckSummary = {
  code: string;
  label: string;
  passed: boolean;
};

export type ShortlistSendReadiness = {
  canSend: boolean;
  tone: "neutral" | "warning";
  title: string;
  detail: string;
  nextAction: string;
  blockedItems: string[];
};

export function isShortlistBuilderEditable(status: string): boolean {
  return SHORTLIST_BUILDER_EDITABLE_STATUSES.includes(
    status as (typeof SHORTLIST_BUILDER_EDITABLE_STATUSES)[number],
  );
}

export function canSaveShortlistBuilder(
  currentStatus: string,
  nextStatus: string,
  title: string,
): boolean {
  return title.trim().length > 0
    && isShortlistBuilderEditable(currentStatus)
    && isShortlistBuilderEditable(nextStatus);
}

export function describeShortlistSendReadiness(
  status: string,
  preSendChecks: ShortlistPreSendCheckSummary[],
): ShortlistSendReadiness {
  const blockedChecks = preSendChecks.filter((check) => !check.passed);
  const blockedItems = blockedChecks.map((check) => `${check.code} · ${check.label}`);
  const checksPassed = blockedChecks.length === 0;
  const statusReady = status === "ready_for_review";

  if (checksPassed && statusReady) {
    return {
      canSend: true,
      tone: "neutral",
      title: "Shortlist is ready to send",
      detail: "All pre-send checks passed and the shortlist is ready_for_review.",
      nextAction: "Approve the send action to create the client-visible workflow event.",
      blockedItems,
    };
  }

  const blockedCount = blockedChecks.length;
  const blockedPhrase = blockedCount === 0
    ? "No blocked pre-send checks remain"
    : `Resolve ${blockedCount} blocked pre-send ${blockedCount === 1 ? "check" : "checks"}`;
  const statusPhrase = statusReady
    ? "keep the shortlist in ready_for_review"
    : "move the shortlist to ready_for_review";
  const firstBlocked = blockedChecks[0];

  return {
    canSend: false,
    tone: "warning",
    title: "Shortlist send is blocked",
    detail: `${blockedPhrase}, then ${statusPhrase}.`,
    nextAction: firstBlocked
      ? `Start with ${firstBlocked.code}: ${firstBlocked.label}.`
      : "Move the shortlist to ready_for_review after consultant review.",
    blockedItems,
  };
}

export function describeWorkflowPageWindow(itemCount: number, offset: number): string {
  if (itemCount <= 0) {
    return "Showing 0 events in this window";
  }
  const start = offset + 1;
  const end = offset + itemCount;
  return `Showing ${start}-${end} events in this window`;
}

export function describeWorkflowTransition(item: ConsultantWorkflowEvent): string {
  const hasPrimaryTransition = hasStatusChange(item.beforeStatus, item.afterStatus);
  const hasCardTransition = hasStatusChange(item.beforeCardStatus, item.afterCardStatus);
  const before = !hasPrimaryTransition && hasCardTransition
    ? item.beforeCardStatus
    : item.beforeStatus ?? item.beforeCardStatus;
  const after = !hasPrimaryTransition && hasCardTransition
    ? item.afterCardStatus
    : item.afterStatus ?? item.afterCardStatus;
  return `${before ?? "unknown"} -> ${after ?? "unknown"}`;
}

function hasStatusChange(before?: string | null, after?: string | null): boolean {
  return typeof before === "string"
    && typeof after === "string"
    && before.length > 0
    && after.length > 0
    && before !== after;
}
