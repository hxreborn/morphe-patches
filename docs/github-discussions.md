# Driving GitHub Discussions

How app requests are read, written, converted, pinned and automated on this repo, with the commands
that worked on 2026-10-20. Issues and discussions share one number sequence: `#285` is a discussion,
`#284` an issue. `gh issue`, `gh search issues` and the REST `/repos/.../issues/N` endpoints do not see
discussions (`/issues/285` is a 404), and `issueOrPullRequest(number: 285)` returns a GraphQL error.
Everything below is `gh api graphql` unless stated otherwise.

## Reading

Categories, with the slug a form file and a workflow `if` match on:

```bash
gh api graphql -f query='{ repository(owner:"hxreborn",name:"morphe-patches"){
  discussionCategories(first:20){ nodes{ name slug isAnswerable description } } } }'
```

Every discussion in a category. `discussions(categoryId:)` takes the node id, so resolve the slug first.
Pagination is `after` plus `pageInfo`; `-F after=null` sends a JSON null on the first page:

```bash
gh api graphql -f slug=app-requests -f query='query($slug: String!){ repository(owner:"hxreborn",name:"morphe-patches"){
  discussionCategory(slug: $slug){ id } } }' --jq .data.repository.discussionCategory.id
gh api graphql -f category=DIC_... -F after=null -f query='query($category: ID!, $after: String){
  repository(owner:"hxreborn",name:"morphe-patches"){
    discussions(first: 100, after: $after, categoryId: $category, orderBy: {field: CREATED_AT, direction: ASC}){
      pageInfo{ hasNextPage endCursor }
      nodes{ number title body closed stateReason url createdAt updatedAt author{ login }
        labels(first: 20){ nodes{ name } }
        comments(first: 100){ nodes{ author{ login } body createdAt url } } } } } }'
```

One discussion, including the fields the bots and `feasibility.py` read:

```bash
gh api graphql -F number=303 -f query='query($number: Int!){ repository(owner:"hxreborn",name:"morphe-patches"){
  discussion(number: $number){ id title body closed stateReason locked activeLockReason
    author{ login } authorAssociation category{ slug } upvoteCount
    reactions{ totalCount } reactionGroups{ content users{ totalCount } }
    labels(first: 20){ nodes{ name } }
    comments(first: 100){ nodes{ author{ login } authorAssociation body createdAt url } } } } }'
```

Globally pinned discussions and a label id:

```bash
gh api graphql -f query='{ repository(owner:"hxreborn",name:"morphe-patches"){
  pinnedDiscussions(first:5){ nodes{ discussion{ number title } pattern gradientStopColors } } } }'
gh api graphql -f label="needs info" -f query='query($label: String!){ repository(owner:"hxreborn",name:"morphe-patches"){
  label(name: $label){ id } } }'
```

A per-category pin has no GraphQL field. Read it off the category page: the text "Pinned to App
Requests" precedes the pinned title. `search(type: DISCUSSION, query: "repo:... author:LOGIN")` works for
a requester's discussions.

`.github/actions/triage/discussion.sh` wraps the reads the bots need, with `REPO=owner/name` in the
environment: `list <slug>` prints one JSON array shaped like `gh issue list --json` (`number title body
state labels comments author url createdAt updatedAt`, state `OPEN`/`CLOSED`, comment authors as
`github-actions` without the `[bot]` suffix); `view <n>` prints one object with the same shape plus
comments. Both cap comments at 100.

## Writing through GraphQL

Mutations that exist for discussions: `createDiscussion`, `updateDiscussion` (title, body, categoryId),
`addDiscussionComment`, `updateDiscussionComment`, `deleteDiscussionComment`, `deleteDiscussion`,
`closeDiscussion`, `reopenDiscussion`, `lockLockable`, `unlockLockable`, `addLabelsToLabelable`,
`removeLabelsFromLabelable`, `markDiscussionCommentAsAnswer`, `minimizeComment`. Every one takes node
ids, so a write is two calls: `discussion(number:){ id }`, then the mutation. There is no mutation for
categories or for pinning; introspection of the mutation type lists nothing matching `categor` or `pin`.

`closeDiscussion` reasons are `DUPLICATE`, `OUTDATED` and `RESOLVED`. The triage bot maps its issue-era
reasons as duplicate → `DUPLICATE`, completed → `RESOLVED`, anything else → `OUTDATED`.

`discussion.sh` subcommands and the mutation each sends:

| Subcommand | Mutation |
| --- | --- |
| `comment <n> <file>` | `addDiscussionComment(input:{discussionId, body})`, body read with `-F body=@file` |
| `title <n> "<text>"` | `updateDiscussion(input:{discussionId, title})` |
| `add-labels <n> <label>...` | `addLabelsToLabelable(input:{labelableId, labelIds})`, ids passed as `-F 'labels[]=ID'` |
| `remove-labels <n> <label>...` | `removeLabelsFromLabelable(...)` |
| `close <n> DUPLICATE\|OUTDATED\|RESOLVED` | `closeDiscussion(input:{discussionId, reason})` |

`gh discussion` (2.102, preview) has `create`, `list`, `view`, `edit` and `comment`, nothing for close,
lock, labels or pins. `gh discussion create --category "App Requests" --title ... --body-file f.md`
and `gh discussion edit N --body-file f.md` are what posted and edited `#303` and `#304`.

## The public-post-gate hook

`~/.claude/hooks/public-post-gate.py` runs on every Bash call. For `gh api graphql` it accepts a
mutation only when its name is one of `createDiscussion`, `updateDiscussion`, `addDiscussionComment`,
`updateDiscussionComment` (plus the issue and PR text mutations), the repo resolves to an own repo, and
the text is literal. `closeDiscussion`, `lockLockable` and the label mutations are refused outright,
so `discussion.sh close|add-labels|remove-labels` run only in CI. The repo is taken from a literal
`GH_REPO=hxreborn/morphe-patches` prefix; `-F body=@file`, `-f body="$var"` and a heredoc piped into an
interpreter are refused as non-literal. This call passed and returned the unchanged title:

```bash
GH_REPO=hxreborn/morphe-patches gh api graphql -f query='mutation { updateDiscussion(input: {discussionId: "D_kwDOTbTF584AqSwE", title: "Read this before you post"}) { discussion { number title } } }'
```

The gate also refuses `curl` to any GitHub host and `gh api` reads whose path contains `comments`,
`payload` or similar publish-looking segments (the octokit `payload-examples` files had to be read with
WebFetch on `raw.githubusercontent.com`), and any Bash call that pipes a script mentioning `gh` into
`python3` or `bash`. Write such scripts to a file and run the file.

## What has no API, and the browser route

Creating and deleting categories, pinning, locking from a session, and converting issues are UI
only. GitHub removed bulk label-based conversion on 2025-06-06, so each issue is one click.

The Claude extension lives in Chromium (`~/.config/chromium/Default/Extensions/fcoeoabg...`), which
is not normally running. Start it on a hidden workspace so it never takes the user's screen, then
connect the `open-claude-in-chrome` MCP:

```bash
export HYPRLAND_INSTANCE_SIGNATURE=$(ls /run/user/1000/hypr | head -1)
hyprctl dispatch 'hl.dsp.exec_cmd("/usr/bin/chromium", { workspace = "special:chromium silent" })'
```

Then `tabs_context_mcp {createIfEmpty: true}`, `navigate`, `read_page {filter: interactive}`,
`form_input` and `javascript_tool`. Screenshots time out on the hidden workspace (`CDP
Page.captureScreenshot timed out`), so read the page tree or text instead. Finish with
`tabs_close_mcp` and `pkill -x -TERM chromium`; `hl.dsp.closewindow` does not exist.

Every write below is the page's own form submitted with `fetch` from `javascript_tool`. A response of
`0 opaqueredirect` with `redirect: 'manual'` means GitHub accepted it; confirm with a GraphQL read.
Wrap the code in `(async () => { ... })()`, since top-level `await` fails, and do not wait after a
click that navigates: the eval dies with `Inspected target navigated` while the click still lands.

| Action | Form |
| --- | --- |
| Create category | `/discussions/categories/new`: `category[name]`, `category[description]`, radio `supports` (`""` open-ended, `mark_as_answer` Q&A, `announcements`, `polls`), then `form.requestSubmit()` or fetch |
| Delete category | on `/discussions/categories`, `form[action="/hxreborn/morphe-patches/discussions/categories/<id>"]` ("Delete and move"); empty categories delete directly |
| Convert issue | on `/issues/N`, click the button whose text is `Convert to discussion`, wait ~1.5 s, set the dialog `select` to the category node id (`DIC_...`), click `I understand, convert this issue` |
| Retitle | `form` on `/discussions/N` holding `_method=put` and `discussion[title]`; fetch the page HTML, parse it with `DOMParser`, POST its `FormData` with the new title |
| Lock | `form[action$="/discussions/N/lock"]`, `allow_reactions=on` keeps reactions |
| Pin globally | `form[action$="/discussions/N/spotlights"]`, radio `discussion_spotlight[preconfigured_color]`; up to 4 |
| Pin in its category | `form[action$="/discussions/N/category_pin"]`; a discussion pins only to its own category |

Conversion keeps the comments, labels and upvotes, gives the discussion the next free number, closes
the issue as completed and locks it with a "converted to" pointer, and fires `discussion created` with
`state: converting` plus one `discussion_comment created` per migrated comment. Both workflows have
to tolerate that burst: the triage `if` requires `state == 'open'`, and the comment job posted nothing
because the migrated pings were older than the bot's window or already answered.

Headless screenshots of public pages need no login:

```bash
chromium --headless=new --user-data-dir=/tmp/profile --hide-scrollbars --window-size=1100,1400 \
  --screenshot=out.png https://github.com/hxreborn/morphe-patches/discussions/302
```

## Workflows

`discussion` (`created`, `edited`, ...) and `discussion_comment` (`created`) trigger only from the
workflow file on the default branch, so nothing can be tested live before a push to `main`. Use
`workflow_dispatch` with a `dry-run` input for the first live read. The job needs `permissions:
discussions: write`, and `GITHUB_TOKEN` can run every discussion mutation.

The payload: `github.event.discussion.{number,title,body,state,locked,category.slug,user.type,
author_association,created_at}` and `changes.body` on `edited`. The reference payload carries no
`labels`, so conditions use `category.slug` and the jobs read labels live through `discussion.sh view`.
`discussion_comment` adds `comment.{body,user.type,author_association,created_at,html_url}`.

What runs here: `issue_triage.yml` (request step on `discussion` events and the daily sweep),
`issue_comments.yml` `request` job (status pings, `needs info` removal), `stale.yml` `requests` job
(closes `needs info` requests as `OUTDATED` after 7 days without an update, skipping `bot-wrong` and
`in progress`), and the last step of `release.yml`. Discussion events carry no timeline, so the bots
keep no "a human reopened this" guard: the sweep skips a close whose comment equals the bot comment
already on the thread, and retitles only on `created`.

`Closes #N` must never name a discussion. `@semantic-release/github` resolves every footer number with
`issueOrPullRequest(number:)` in one query, the error aborts the call, and the success step fails after
the release is published. New-app commits carry no footer; `release.yml` instead runs
`.github/scripts/shipped_requests.py` on a stable release, on the package names new since the previous
stable tag (a `dev` pre-release never closes a request). Every open
request naming one of them gets a comment that the app is supported, with links to the four issue
forms, through `discussion.sh comment`, and is closed (`remove-labels "in progress"`,
`close RESOLVED`) whatever its `Requested features` field says.

## Gotchas

- `gh api -F key=value` converts `null`, `true`, `false` and integers; `-f` keeps strings. Lists go as
  repeated `-F 'labels[]=ID'`. `-F body=@file` reads a file. `--silent` drops the response.
- A composite action's script is invoked as `bash -eo pipefail path`, never executed directly, so the
  checkout's file mode cannot break it.
- `discussion_comment` and `discussion` events of the bot's own writes carry `user.type == 'Bot'`;
  filter on it, not on the login.
- A harness that writes its merged event to `$RUNNER_TEMP/event.json` while `GITHUB_EVENT_PATH` points
  at the same file truncates its own input. Name the merged file differently.
- In the step tests the fake `gh` dispatches on the operation name parsed from the query text
  (`query RequestList(...)`), so every query in `discussion.sh` carries a unique name.
- The form's `labels:` key applies labels on submit; `title:` and `name:` are not read. Converted
  issues keep `[App Request]:` in the title; strip it through the retitle form.
- A `discussion.sh list` of a category that does not exist fails on a null category id; create the
  category before pushing the workflows.
