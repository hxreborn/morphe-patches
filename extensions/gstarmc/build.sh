#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
resources=$here/../../patches/src/main/resources/gstarmc

ndk_version=${NDK_VERSION:-27.3.13750724}
build_tools_version=${BUILD_TOOLS_VERSION:-37.0.0}
platform_version=${PLATFORM_VERSION:-36}

sdk=${ANDROID_HOME:-$HOME/Android/Sdk}
build_tools=${BUILD_TOOLS:-$sdk/build-tools/$build_tools_version}
platform=${PLATFORM:-$sdk/platforms/android-$platform_version/android.jar}
toolchain=${NDK:-$sdk/ndk/$ndk_version}/toolchains/llvm/prebuilt/linux-x86_64/bin
javac=${JAVAC:-/usr/lib/jvm/java-21-openjdk/bin/javac}

die() {
    echo "$*" >&2
    exit 1
}

[ -x "$build_tools/d8" ] || die "no d8 at $build_tools; install build-tools $build_tools_version or set BUILD_TOOLS"
[ -f "$platform" ] || die "no android.jar at $platform; install platform $platform_version or set PLATFORM"
[ -x "$toolchain/clang" ] || die "no NDK toolchain at $toolchain; install NDK $ndk_version or set NDK"
[ -x "$javac" ] || die "no javac at $javac"
javac_version=$("$javac" -version 2>&1 | cut -d' ' -f2)
[ "${javac_version%%.*}" = 21 ] || die "javac $javac_version at $javac is not JDK 21"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

mkdir -p "$resources"

"$javac" -nowarn -source 8 -target 8 -bootclasspath "$platform" -d "$work/boot" "$here"/src/main/java/Boot.java
"$build_tools/d8" --min-api 23 --lib "$platform" --output "$work/boot" "$work"/boot/*.class
cp "$work/boot/classes.dex" "$resources/boot.dex"

"$toolchain/aarch64-linux-android23-clang" -O2 -fPIC -shared -s -o "$resources/hiddenapi.so" "$here/hiddenapi.c"

ls -la "$resources"
