#!/usr/bin/env bash
set -eo pipefail

ISSUE=$1
shift
source "$(dirname "${BASH_SOURCE[0]}")/issue_changes.sh"
for label in "$@"; do
  added_by=$(gh api "repos/$REPO/issues/$ISSUE/timeline" --paginate \
    --jq ".[] | select(.event == \"labeled\" and .label.name == \"$label\") | .actor.login" | tail -n 1)
  [ "$added_by" != "github-actions[bot]" ] || change_issue edit --remove-label "$label"
done
