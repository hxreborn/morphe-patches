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
clang=${NDK:-$sdk/ndk/$ndk_version}/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android23-clang

die() {
    echo "$*" >&2
    exit 1
}

[ -x "$build_tools/d8" ] || die "no d8 at $build_tools; install build-tools $build_tools_version or set BUILD_TOOLS"
[ -f "$platform" ] || die "no android.jar at $platform; install platform $platform_version or set PLATFORM"
[ -x "$clang" ] || die "no clang at $clang; install NDK $ndk_version or set NDK"
command -v javac >/dev/null || die "javac is not on PATH"

verify=false
[ "${1:-}" = "--verify" ] && verify=true

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

out=$resources
fixture_out=$fixtures
if $verify; then
    out=$work/resources
    fixture_out=$work/fixtures
fi
mkdir -p "$out" "$fixture_out"

javac -Xlint:all -source 8 -target 8 -bootclasspath "$platform" -d "$work/boot" "$here"/src/hx/*.java
"$build_tools/d8" --min-api 28 --lib "$platform" --output "$work/boot" "$work"/boot/hx/*.class
cp "$work/boot/classes.dex" "$out/boot.dex"

rm -f "$out"/libhxpatch*.so
for header in "$here"/native/profiles/*.h; do
    profile=$(basename "$header" .h)
    "$clang" -O2 -fPIC -shared -Wall -Wextra -Werror -I"$here/native" \
        -DHX_PROFILE="\"profiles/$profile.h\"" \
        -o "$out/libhxpatch-$profile.so" "$here/native/hxpatch.c" -llog
done

# string_ids beyond the opaque block required for head recovery tests
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
}
JAVA
} > "$work/fixture/sample/Fixture.java"

javac -nowarn -source 8 -target 8 -bootclasspath "$platform" -d "$work/fixture/classes" \
    "$work/fixture/sample/Fixture.java"
"$build_tools/d8" --min-api 23 --lib "$platform" --output "$work/fixture" \
    "$work"/fixture/classes/sample/*.class
cp "$work/fixture/classes.dex" "$fixture_out/sample.dex"

{
    echo "ndk $ndk_version"
    echo "build-tools $build_tools_version"
    echo "platform android-$platform_version"
    echo "javac $(javac -version 2>&1 | cut -d' ' -f2)"
    ( cd "$here" && sha256sum native/hxpatch.c native/profiles/*.h src/hx/*.java )
    ( cd "$out" && sha256sum boot.dex libhxpatch-*.so )
    ( cd "$fixture_out" && sha256sum sample.dex )
} > "$out/provenance.txt"

if $verify; then
    for built in boot.dex provenance.txt $(cd "$out" && ls libhxpatch-*.so); do
        cmp -s "$resources/$built" "$out/$built" ||
            die "$built does not match its sources; run extensions/ijiami/build.sh"
    done
    cmp -s "$fixtures/sample.dex" "$fixture_out/sample.dex" ||
        die "sample.dex does not match its generator; run extensions/ijiami/build.sh"
    echo "checked-in binaries match their sources"
else
    ls -la "$resources" "$fixtures"
fi
