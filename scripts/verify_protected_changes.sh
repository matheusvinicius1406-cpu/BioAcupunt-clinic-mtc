#!/usr/bin/env bash
set -euo pipefail

# Script to detect modifications to protected build files compared to origin/main
# Exit with non-zero if protected files are modified.

PROTECTED_PATTERNS=(
  "app/"
  "build.gradle.kts"
  "settings.gradle.kts"
  "gradle/"
  "gradle.properties"
  "local.defaults.properties"
  "local.properties"
  ".github/workflows/"
  "backend/"
)

git fetch origin main --quiet || true
MODIFIED_FILES=$(git diff --name-only origin/main...HEAD || true)

if [ -z "$MODIFIED_FILES" ]; then
  echo "No modified files detected compared to origin/main."
  exit 0
fi

echo "Modified files:"$'
'$MODIFIED_FILES

while IFS= read -r file; do
  for pattern in "${PROTECTED_PATTERNS[@]}"; do
    if [[ "$file" == "$pattern"* || "$file" == "$pattern" ]]; then
      echo "Protected file modified: $file"
      echo "Changes to build-critical files must be done via PR and reviewed by maintainers."
      exit 1
    fi
  done
done <<< "$MODIFIED_FILES"

echo "No protected files modified."
exit 0
