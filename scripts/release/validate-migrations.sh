#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

printf '[release:migrations] Checking Flyway migration filename versions and order.\n'
node <<'NODE'
const fs = require("fs");
const path = require("path");

const migrationDir = path.join("services", "core-api", "src", "main", "resources", "db", "migration");
const files = fs.readdirSync(migrationDir)
  .filter((file) => file.endsWith(".sql"))
  .sort((a, b) => a.localeCompare(b));

if (files.length === 0) {
  throw new Error("No Flyway migration files found.");
}

const versions = [];
const seen = new Map();
for (const file of files) {
  const match = /^V([0-9]+)__[A-Za-z0-9_]+\.sql$/.exec(file);
  if (!match) {
    throw new Error(`Invalid Flyway migration filename: ${file}`);
  }
  const version = Number(match[1]);
  versions.push(version);
  if (seen.has(version)) {
    throw new Error(`Duplicate Flyway migration version V${version}: ${seen.get(version)} and ${file}`);
  }
  seen.set(version, file);
}

const sortedVersions = [...versions].sort((a, b) => a - b);
for (let index = 0; index < sortedVersions.length; index += 1) {
  const expected = index + 1;
  if (sortedVersions[index] !== expected) {
    throw new Error(`Flyway migration versions must be contiguous from V1; expected V${expected}, got V${sortedVersions[index]}`);
  }
}

console.log(`[release:migrations] ${files.length} migrations are contiguous from V1 to V${sortedVersions.at(-1)}.`);
NODE

printf '[release:migrations] Running Testcontainers Flyway apply/validation coverage against PostgreSQL.\n'
mvn -f services/core-api/pom.xml -Dtest=TruthLayerPostgresMigrationIntegrationTest test
