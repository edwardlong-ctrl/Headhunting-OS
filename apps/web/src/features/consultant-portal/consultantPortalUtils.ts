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
