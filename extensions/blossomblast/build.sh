#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ndk=${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/27.3.13750724}
toolchain=$ndk/toolchains/llvm/prebuilt/linux-x86_64
compiler=$toolchain/bin/aarch64-linux-android24-clang++
readelf=$toolchain/bin/llvm-readelf
output=$here/../../patches/src/main/resources/blossomblast/libblossomblast.so
page_size=16384

test -x "$compiler"
mkdir -p "$(dirname "$output")"
"$compiler" -std=c++17 -O2 -fPIC -shared -fvisibility=hidden -static-libstdc++ \
    -Wl,-z,max-page-size=$page_size -Wl,-z,common-page-size=$page_size \
    -I"$toolchain/sysroot/usr/include" "$here/src/blossomblast.cpp" -ldl -o "$output"

"$readelf" -lW "$output" | awk -v align=$(printf '0x%x' $page_size) '
    $1 == "LOAD" { loads++; if ($NF != align) bad = bad " LOAD " $NF }
    $1 == "GNU_RELRO" { relro = 1; end = strtonum($3) + strtonum($6); if (end % strtonum(align) != 0) bad = bad " RELRO end " end }
    END { if (loads == 0 || !relro || bad != "") { print "bridge alignment check failed:" bad > "/dev/stderr"; exit 1 } }'
