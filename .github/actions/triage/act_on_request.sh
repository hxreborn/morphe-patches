#!/usr/bin/env bash
set -eo pipefail

n=$1
list=$2
here=$(dirname "$0")
discussion() { bash -eo pipefail "$here/discussion.sh" "$@"; }

jq -e --argjson n "$n" 'any(.[]; .number == $n and .state == "OPEN")' requests.json >/dev/null || exit 0
jq -e --argjson n "$n" '.[] | select(.number == $n) | .labels | map(.name) | index("bot-wrong")' \
  requests.json >/dev/null && exit 0
jq -r --argjson n "$n" '.[] | select(.number == $n) | .body // ""' requests.json > body.md
previous=$(jq -r --argjson n "$n" '[.[] | select(.number == $n) | .comments[]
  | select(.author.login == "github-actions" and (.body | contains("<!-- app-request-bot"))) | .body] | last // empty' \
  requests.json)
printf '%s' "$previous" > previous.md
python3 "$here/app_request.py" "$n" body.md requests.json "$list" bundles.json elsewhere previous.md > verdict.json
verdict=$(jq -r .verdict verdict.json)
if [ "$verdict" = hold ]; then
  echo "#$n: held while people discuss it"
  exit 0
fi
title=$(jq -r .title verdict.json)
current=$(jq -r --argjson n "$n" '.[] | select(.number == $n) | .title' requests.json)
facts=$(jq -r .facts verdict.json)
jq -j .comment verdict.json > comment.md
if [ -n "$previous" ] && [ ! -s bundles.json ]; then
  echo "#$n: skipped while the bundle registry is unavailable"
  exit 0
fi
if [ "$verdict" = pass ] && [ ! -s comment.md ] && [ -n "$previous" ]; then
  printf '<!-- app-request-bot facts=%s -->\n%s\n' "$facts" "$(python3 "$here/replies.py" request-passes)" > comment.md
fi
previous_facts=$(head -n 1 <<< "$previous" | sed -n 's/^<!-- app-request-bot facts=\([0-9a-f]*\) -->$/\1/p')
if [ -n "$previous_facts" ]; then
  [ "$facts" = "$previous_facts" ] && unchanged=true || unchanged=false
elif [ -n "$previous" ] && [ "$verdict" != pass ]; then
  unchanged=true
elif [ -n "$previous" ]; then
  grep -q 'passes the automated checks' <<< "$previous" && unchanged=true || unchanged=false
else
  unchanged=false
fi
[ "$ACTION" != created ] && [ "$verdict" = pass ] && [ -z "$previous" ] && exit 0
if [ "$verdict" = close ]; then
  if [ "$unchanged" = true ]; then
    echo "#$n: already closed once for the same reason and reopened, left alone"
    exit 0
  fi
  original=$(jq -r .duplicate_of verdict.json)
  if [ "$original" != 0 ] && [ "$(discussion view "$original" | jq -r .state)" != OPEN ]; then
    echo "#$n: #$original is no longer open, not closed as its duplicate"
    exit 0
  fi
fi
echo "#$n: $(jq -c 'del(.comment)' verdict.json) unchanged=$unchanged"
[ "$DRY_RUN" = true ] && exit 0

if [ "$ACTION" = created ] && [ "$title" != "$current" ]; then
  discussion title "$n" "$title"
fi
if [ -s comment.md ] && [ "$unchanged" != true ]; then
  mapfile -t labels < <(jq -r '.labels[]' verdict.json)
  [ "${#labels[@]}" -eq 0 ] || discussion add-labels "$n" "${labels[@]}"
  discussion comment "$n" comment.md
fi
if [ -n "$previous" ]; then
  mapfile -t outdated < <(jq -r --argjson n "$n" --slurpfile verdict verdict.json \
    '.[] | select(.number == $n) | .labels[].name | select(. == "needs info" or . == "duplicate")
     | select(. as $label | $verdict[0].labels | index($label) | not)' requests.json)
  [ "${#outdated[@]}" -eq 0 ] || discussion remove-labels "$n" "${outdated[@]}"
fi
if [ "$verdict" = close ]; then
  case "$(jq -r .reason verdict.json)" in
    duplicate) reason=DUPLICATE ;;
    completed) reason=RESOLVED ;;
    *) reason=OUTDATED ;;
  esac
  discussion close "$n" "$reason"
fi
