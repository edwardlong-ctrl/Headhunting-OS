#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

printf '[release:ai-eval] Validating local AI eval artifacts, prompts, and schemas without live model calls.\n'
node <<'NODE'
const fs = require("fs");
const path = require("path");

const evalDir = path.join("services", "core-api", "src", "main", "resources", "ai", "evals");
const promptDir = path.join("services", "core-api", "src", "main", "resources", "ai", "prompts");
const schemaDir = path.join("services", "core-api", "src", "main", "resources", "ai", "schemas");
const requiredAssertions = [
  "output_conforms_to_schema",
  "no_model_output_as_fact",
  "no_unauthorized_write_back",
  "no_client_identity_leakage",
];

const files = fs.readdirSync(evalDir)
  .filter((file) => file.endsWith("-eval-cases.json"))
  .sort();

if (files.length === 0) {
  throw new Error("AI eval regression suite is empty.");
}

for (const file of files) {
  const fullPath = path.join(evalDir, file);
  const raw = fs.readFileSync(fullPath, "utf8");
  const parsed = JSON.parse(raw);
  const expectedTaskKey = file.replace(/-eval-cases\.json$/, "");

  if (parsed.taskKey !== expectedTaskKey) {
    throw new Error(`${file} taskKey ${parsed.taskKey} does not match filename key ${expectedTaskKey}`);
  }
  if (!String(parsed.suiteVersion || "").startsWith(`${parsed.taskKey}.eval.`)) {
    throw new Error(`${file} suiteVersion must start with ${parsed.taskKey}.eval.`);
  }
  if (!Array.isArray(parsed.requiredAssertions) || parsed.requiredAssertions.length === 0) {
    throw new Error(`${file} requiredAssertions must be a non-empty array.`);
  }
  for (const assertion of requiredAssertions) {
    if (!parsed.requiredAssertions.includes(assertion)) {
      throw new Error(`${file} is missing required assertion ${assertion}`);
    }
  }
  if (!Array.isArray(parsed.cases) || parsed.cases.length === 0) {
    throw new Error(`${file} cases must be a non-empty array.`);
  }
  for (const testCase of parsed.cases) {
    if (!testCase.caseId || !testCase.purpose || !testCase.expectedGate) {
      throw new Error(`${file} contains an eval case without caseId, purpose, or expectedGate.`);
    }
  }

  for (const relatedPath of [
    path.join(promptDir, `${parsed.taskKey}-v1.txt`),
    path.join(schemaDir, `${parsed.taskKey}-input.schema.json`),
    path.join(schemaDir, `${parsed.taskKey}-output.schema.json`),
  ]) {
    if (!fs.existsSync(relatedPath)) {
      throw new Error(`${file} is missing related artifact ${relatedPath}`);
    }
    if (fs.statSync(relatedPath).size === 0) {
      throw new Error(`${relatedPath} is empty.`);
    }
  }
}

console.log(`[release:ai-eval] ${files.length} eval suites validated with prompt/schema coverage.`);
NODE
