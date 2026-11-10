#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
workdir=$(mktemp -d)
trap 'rm -rf -- "$workdir"' EXIT
for name in theme accent; do
 clang -target aarch64-linux-android26 -c "$name.S" -o "$workdir/$name.o"
 ld.lld -m aarch64elf --image-base=0 -Ttext=0 --entry="${name}_hooks" "$workdir/$name.o" -o "$workdir/$name.elf"
 llvm-objcopy -O binary --only-section=.text "$workdir/$name.elf" "../../patches/src/main/resources/ultimea/$name.bin"
 chmod 644 "../../patches/src/main/resources/ultimea/$name.bin"
done
