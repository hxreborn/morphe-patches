#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
resources=$here/../../patches/src/main/resources/moviebox

build_tools_version=${BUILD_TOOLS_VERSION:-37.0.0}
platform_version=${PLATFORM_VERSION:-36}

sdk=${ANDROID_HOME:-$HOME/Android/Sdk}
build_tools=${BUILD_TOOLS:-$sdk/build-tools/$build_tools_version}
platform=${PLATFORM:-$sdk/platforms/android-$platform_version/android.jar}

die() {
    echo "$*" >&2
    exit 1
}

[ -x "$build_tools/d8" ] || die "no d8 at $build_tools; install build-tools $build_tools_version or set BUILD_TOOLS"
[ -f "$platform" ] || die "no android.jar at $platform; install platform $platform_version or set PLATFORM"
command -v javac >/dev/null || die "javac is not on PATH"

verify=false
[ "${1:-}" = "--verify" ] && verify=true

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

out=$resources
$verify && out=$work/resources
mkdir -p "$out"

javac -Xlint:all -source 8 -target 8 -bootclasspath "$platform" -d "$work/hook" "$here"/src/hx/*.java
"$build_tools/d8" --min-api 28 --lib "$platform" --output "$work/hook" "$work"/hook/hx/*.class
cp "$work/hook/classes.dex" "$out/dashhook.dex"

{
    echo "build-tools $build_tools_version"
    echo "platform android-$platform_version"
    echo "javac $(javac -version 2>&1 | cut -d' ' -f2)"
    ( cd "$here" && sha256sum src/hx/*.java )
    ( cd "$out" && sha256sum dashhook.dex )
} > "$out/provenance.txt"

if $verify; then
    for built in dashhook.dex provenance.txt; do
        cmp -s "$resources/$built" "$out/$built" ||
            die "$built does not match its sources; run extensions/moviebox/build.sh"
    done
    echo "checked-in binaries match their sources"
else
    ls -la "$resources"
fi
