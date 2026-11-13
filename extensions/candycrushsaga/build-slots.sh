#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ndk=${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/27.3.13750724}
compiler=$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang++
include=$ndk/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include
output=$here/../../patches/src/main/resources/candycrushsaga/libccsslots.so

test -x "$compiler"
mkdir -p "$(dirname "$output")"
"$compiler" -std=c++17 -O2 -fPIC -shared -fvisibility=hidden -static-libstdc++ -I"$include" \
    "$here/src/slots.cpp" -llog -ldl -o "$output"
