#!/usr/bin/env bash
set -uo pipefail

log="${RUNNER_TEMP:-/tmp}/gradle-tests.log"
attempts=3

for attempt in $(seq 1 "$attempts"); do
  ./gradlew "$@" --no-daemon 2>&1 | tee "$log"
  status=${PIPESTATUS[0]}
  if [ "$status" -eq 0 ]; then
    exit 0
  fi
  if ! grep -qE 'MavenArtifactFetcher|Received status code (429|5[0-9][0-9])' "$log"; then
    exit "$status"
  fi
  echo "::warning::A dependency download failed (attempt $attempt of $attempts)"
  sleep 30
done
exit "$status"
