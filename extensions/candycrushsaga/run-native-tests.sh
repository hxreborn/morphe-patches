#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
output=$(mktemp)
trap 'rm -f "$output"' EXIT
"${CXX:-c++}" -std=c++17 -O2 -Wall -Wextra -Werror "$here/tests/RedirectsTest.cpp" -o "$output"
"$output"
