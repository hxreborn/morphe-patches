#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
resources=$here/../../patches/src/main/resources/moviebox

build_tools_version=${BUILD_TOOLS_VERSION:-37.0.0}
platform_version=${PLATFORM_VERSION:-36}

sdk=${ANDROID_HOME:-$HOME/Android/Sdk}
build_tools=${BUILD_TOOLS:-$sdk/build-tools/$build_tools_version}
platform=${PLATFORM:-$sdk/platforms/android-$platform_version/android.jar}
javac=${JAVAC:-/usr/lib/jvm/java-21-openjdk/bin/javac}

die() {
    echo "$*" >&2
    exit 1
}

[ -x "$build_tools/d8" ] || die "no d8 at $build_tools; install build-tools $build_tools_version or set BUILD_TOOLS"
[ -f "$platform" ] || die "no android.jar at $platform; install platform $platform_version or set PLATFORM"
[ -x "$javac" ] || die "no javac at $javac"
javac_version=$("$javac" -version 2>&1 | cut -d' ' -f2)
[ "${javac_version%%.*}" = 21 ] || die "javac $javac_version at $javac is not JDK 21"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

mkdir -p "$resources"

"$javac" -Xlint:all -source 8 -target 8 -bootclasspath "$platform:$build_tools/core-lambda-stubs.jar" -d "$work/hook" $(find "$here/src" -name '*.java' | sort)
"$build_tools/d8" --min-api 23 --lib "$platform" --output "$work/hook" $(find "$work/hook" -name '*.class' | sort)
cp "$work/hook/classes.dex" "$resources/dashhook.dex"

ls -la "$resources"
