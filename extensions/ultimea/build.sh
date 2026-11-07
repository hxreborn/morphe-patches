#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
workdir=$(mktemp -d)
trap 'rm -rf -- "$workdir"' EXIT
clang -target aarch64-linux-android26 -c theme.S -o "$workdir/theme.o"
ld.lld -m aarch64elf --image-base=0 -Ttext=0 --entry=theme_hooks "$workdir/theme.o" -o "$workdir/theme.elf"
llvm-objcopy -O binary --only-section=.text "$workdir/theme.elf" ../../patches/src/main/resources/ultimea/theme.bin
chmod 644 ../../patches/src/main/resources/ultimea/theme.bin
