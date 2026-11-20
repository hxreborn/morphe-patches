#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
resources=$here/../../patches/src/main/resources/ijiami
fixtures=$here/../../patches/src/test/resources/ijiami

ndk_version=${NDK_VERSION:-27.3.13750724}
build_tools_version=${BUILD_TOOLS_VERSION:-37.0.0}
platform_version=${PLATFORM_VERSION:-36}

sdk=${ANDROID_HOME:-$HOME/Android/Sdk}
build_tools=${BUILD_TOOLS:-$sdk/build-tools/$build_tools_version}
platform=${PLATFORM:-$sdk/platforms/android-$platform_version/android.jar}
toolchain=${NDK:-$sdk/ndk/$ndk_version}/toolchains/llvm/prebuilt/linux-x86_64/bin

die() {
    echo "$*" >&2
    exit 1
}

[ -x "$build_tools/d8" ] || die "no d8 at $build_tools; install build-tools $build_tools_version or set BUILD_TOOLS"
[ -f "$platform" ] || die "no android.jar at $platform; install platform $platform_version or set PLATFORM"
[ -x "$toolchain/clang" ] || die "no NDK toolchain at $toolchain; install NDK $ndk_version or set NDK"
command -v javac >/dev/null || die "javac is not on PATH"
javac_version=$(javac -version 2>&1 | cut -d' ' -f2)
[ "${javac_version%%.*}" = 21 ] || die "javac $javac_version is not JDK 21"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

mkdir -p "$resources" "$fixtures"

javac -Xlint:all -source 8 -target 8 -bootclasspath "$platform" -d "$work/boot" "$here"/src/main/java/hx/*.java
"$build_tools/d8" --min-api 28 --lib "$platform" --output "$work/boot" "$work"/boot/hx/*.class
cp "$work/boot/classes.dex" "$resources/boot.dex"

rm -f "$resources"/libhxpatch*.so
for header in "$here"/native/profiles/*.h; do
    profile=$(basename "$header" .h)
    case $(sed -n 's/^#define HX_ABI "\(.*\)"$/\1/p' "$header") in
        arm64-v8a) clang=$toolchain/aarch64-linux-android23-clang ;;
        armeabi-v7a) clang=$toolchain/armv7a-linux-androideabi23-clang ;;
        *) die "$header declares no supported HX_ABI" ;;
    esac
    "$clang" -O2 -fPIC -shared -Wall -Wextra -Werror -I"$here/native" \
        -DHX_PROFILE="\"profiles/$profile.h\"" \
        -o "$resources/libhxpatch-$profile.so" "$here/native/hxpatch.c" -llog
done

mkdir -p "$work/fixture/sample"
{
    echo 'package sample;'
    echo
    echo 'public class Fixture {'
    for index in $(seq 0 399); do
        printf '    static final String S%d = "fixture string %04d";\n' "$index" "$index"
    done
    cat <<'JAVA'

    public boolean flag() { return S0.length() > 3; }

    public void run() { S1.length(); }

    public String name() { return S2; }

    public int count() { return S3.length(); }

    public boolean tiny() { return true; }

    public boolean over() { return S4.length() > 1; }

    public boolean over(int n) { return n > 1; }

    public Boolean boxed() { return flag() ? Boolean.TRUE : Boolean.FALSE; }

    public Integer boxedCount() { return Integer.valueOf(count()); }

    public String typedCatch() {
        try {
            return S5.substring(1);
        } catch (RuntimeException e) {
            return S6;
        }
    }

    public int catchAll() {
        try {
            return S7.length();
        } finally {
            S8.length();
        }
    }

    public String digits(int n) {
        return String.valueOf(n);
    }
}
JAVA
} > "$work/fixture/sample/Fixture.java"

javac -nowarn -source 8 -target 8 -bootclasspath "$platform" -d "$work/fixture/classes" \
    "$work/fixture/sample/Fixture.java"
"$build_tools/d8" --min-api 23 --lib "$platform" --output "$work/fixture" \
    "$work"/fixture/classes/sample/*.class
cp "$work/fixture/classes.dex" "$fixtures/sample.dex"

ls -la "$resources" "$fixtures"
