#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ndk=${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/27.3.13750724}
compiler=$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang
resources=$here/../../patches/src/main/resources/magicposer

test -x "$compiler"
mkdir -p "$resources"
rm -f "$resources/libmpbridge.so"
for mode in premium tracking; do
    "$compiler" -std=c11 -O2 -fPIC -shared -fvisibility=hidden -Wall -Wextra -Werror \
        -DMPBRIDGE_"$(echo "$mode" | tr a-z A-Z)" "$here/native/mpbridge.c" -llog \
        -o "$resources/libmp$mode.so"
done
