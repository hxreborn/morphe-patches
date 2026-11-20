change_issue() {
  local operation=$1
  shift
  if [ -z "${PREVIEW_ISSUE:-}" ]; then
    gh issue "$operation" "$ISSUE" --repo "$REPO" "$@"
    return
  fi

  {
    case "$operation" in
      comment)
        printf '\nPost comment\n\n<pre>'
        jq -Rrs @html "$2"
        printf '</pre>\n'
        ;;
      edit)
        case "$1" in
          --add-label) printf '\nAdd labels: %s\n' "$2" ;;
          --remove-label) printf '\nRemove label: %s\n' "$2" ;;
          *) return 1 ;;
        esac
        ;;
      close) printf '\nClose as %s\n' "$2" ;;
      reopen) printf '\nReopen issue\n' ;;
      *) return 1 ;;
    esac
  } >> "$GITHUB_STEP_SUMMARY"
}
