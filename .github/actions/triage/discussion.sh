#!/usr/bin/env bash
set -eo pipefail

command=$1
shift
owner=${REPO%/*}
name=${REPO#*/}

graphql() {
  gh api graphql -f owner="$owner" -f name="$name" "$@"
}

discussion_id() {
  graphql -F number="$1" -f query='query DiscussionId($owner: String!, $name: String!, $number: Int!) {
    repository(owner: $owner, name: $name) { discussion(number: $number) { id } } }' \
    --jq '.data.repository.discussion.id'
}

label_args() {
  local label
  for label in "$@"; do
    printf -- '-F\nlabels[]=%s\n' "$(graphql -f label="$label" -f query='query LabelId($owner: String!, $name: String!, $label: String!) {
      repository(owner: $owner, name: $name) { label(name: $label) { id } } }' --jq '.data.repository.label.id')"
  done
}

case "$command" in
  list)
    category=$(graphql -f slug="$1" -f query='query CategoryId($owner: String!, $name: String!, $slug: String!) {
      repository(owner: $owner, name: $name) { discussionCategory(slug: $slug) { id } } }' \
      --jq '.data.repository.discussionCategory.id')
    after=null
    pages=()
    while :; do
      page=$(graphql -f category="$category" -F after="$after" -f query='query RequestList($owner: String!, $name: String!, $category: ID!, $after: String) {
        repository(owner: $owner, name: $name) {
          discussions(first: 100, after: $after, categoryId: $category, orderBy: {field: CREATED_AT, direction: ASC}) {
            pageInfo { hasNextPage endCursor }
            nodes {
              number title body closed url createdAt updatedAt authorAssociation
              author { login }
              labels(first: 20) { nodes { name } }
              comments(first: 100) { nodes { author { login } body createdAt url } }
            }
          }
        }
      }' --jq '.data.repository.discussions')
      pages+=("$page")
      [ "$(jq -r .pageInfo.hasNextPage <<< "$page")" = true ] || break
      after=$(jq -r .pageInfo.endCursor <<< "$page")
    done
    printf '%s\n' "${pages[@]}" | jq -s '[.[].nodes[] | {
      number, title, body, url, createdAt, updatedAt,
      state: (if .closed then "CLOSED" else "OPEN" end),
      author: {login: (.author.login // "")},
      association: .authorAssociation,
      labels: [.labels.nodes[] | {name}],
      comments: [.comments.nodes[] | {author: {login: (.author.login // "")}, body, createdAt, url}]
    }]'
    ;;
  view)
    graphql -F number="$1" -f query='query Discussion($owner: String!, $name: String!, $number: Int!) {
      repository(owner: $owner, name: $name) { discussion(number: $number) {
        number title closed url createdAt author { login }
        labels(first: 20) { nodes { name } }
        comments(first: 100) { nodes { author { login } body createdAt url } } } } }' \
      --jq '.data.repository.discussion | {number, title, url, createdAt,
        state: (if .closed then "CLOSED" else "OPEN" end),
        author: {login: (.author.login // "")},
        labels: [.labels.nodes[] | {name}],
        comments: [.comments.nodes[] | {author: {login: (.author.login // "")}, body, createdAt, url}]}'
    ;;
  comment)
    graphql -f id="$(discussion_id "$1")" -F body="@$2" -f query='mutation AddComment($id: ID!, $body: String!) {
      addDiscussionComment(input: {discussionId: $id, body: $body}) { comment { url } } }' \
      --jq '.data.addDiscussionComment.comment.url'
    ;;
  title)
    graphql -f id="$(discussion_id "$1")" -f title="$2" -f query='mutation Retitle($id: ID!, $title: String!) {
      updateDiscussion(input: {discussionId: $id, title: $title}) { clientMutationId } }' --silent
    ;;
  add-labels)
    number=$1
    shift
    mapfile -t labels < <(label_args "$@")
    graphql -f id="$(discussion_id "$number")" "${labels[@]}" -f query='mutation AddLabels($id: ID!, $labels: [ID!]!) {
      addLabelsToLabelable(input: {labelableId: $id, labelIds: $labels}) { clientMutationId } }' --silent
    ;;
  remove-labels)
    number=$1
    shift
    mapfile -t labels < <(label_args "$@")
    graphql -f id="$(discussion_id "$number")" "${labels[@]}" -f query='mutation RemoveLabels($id: ID!, $labels: [ID!]!) {
      removeLabelsFromLabelable(input: {labelableId: $id, labelIds: $labels}) { clientMutationId } }' --silent
    ;;
  close)
    graphql -f id="$(discussion_id "$1")" -f reason="$2" -f query='mutation Close($id: ID!, $reason: DiscussionCloseReason!) {
      closeDiscussion(input: {discussionId: $id, reason: $reason}) { clientMutationId } }' --silent
    ;;
  lock)
    graphql -f id="$(discussion_id "$1")" -f query='mutation Lock($id: ID!) {
      lockLockable(input: {lockableId: $id, lockReason: RESOLVED}) { clientMutationId } }' --silent
    ;;
  *)
    echo "discussion.sh: unknown command $command" >&2
    exit 2
    ;;
esac
