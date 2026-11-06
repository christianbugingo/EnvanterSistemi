#!/usr/bin/env bash
# Build-only helpers; source after changing directory to InventorySystemi.
setup_target() {
  target="$1"
  host_os=$(uname -s)
  host_arch=$(uname -m)
  case "$target" in
    macos-arm64) expected_os=Darwin; expected_arch=arm64; profile=mac-aarch64; native_classifier=mac-aarch64 ;;
    macos-x86_64) expected_os=Darwin; expected_arch=x86_64; profile=mac-x86_64; native_classifier=mac ;;
    linux-x86_64) expected_os=Linux; expected_arch=x86_64; profile=linux; native_classifier=linux ;;
    *) echo "Unknown packaging target: $target" >&2; exit 2 ;;
  esac
  [[ "$host_os" == "$expected_os" && "$host_arch" == "$expected_arch" ]] || {
    echo "$target requires a native $expected_os $expected_arch host; got $host_os $host_arch. Cross-labeling is refused." >&2; exit 2;
  }
  command -v python3 >/dev/null
  if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ "$host_os" == Darwin && -d ../.tools/jdk/Contents/Home ]]; then
      JAVA_HOME="$(cd ../.tools/jdk/Contents/Home && pwd)"
    elif [[ "$host_os" == Darwin ]]; then
      JAVA_HOME=$(/usr/libexec/java_home -v 21)
    else
      JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")
    fi
  fi
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
  [[ -x "$JAVA_HOME/bin/jpackage" && -x "$JAVA_HOME/bin/javac" ]] || { echo 'A complete JDK 21 with jpackage is required.' >&2; exit 2; }
  settings=$("$JAVA_HOME/bin/java" -XshowSettings:properties -version 2>&1)
  jdk_version=$(printf '%s\n' "$settings" | awk '$1=="java.version" {print $3}')
  jdk_arch=$(printf '%s\n' "$settings" | awk '$1=="os.arch" {print $3}')
  [[ "$jdk_version" == 21.* ]] || { echo "JDK 21 required; got $jdk_version" >&2; exit 2; }
  python3 scripts/package_target.py host "$target" "$jdk_arch"
  project_dir=$(pwd)
  build="$project_dir/target/build-$target"
  stage="$project_dir/target/release-$target"
  mkdir -p target/logs ../.tools/build-home ../.tools/tmp
  exec > >(tee "target/logs/packaging-$target.log") 2>&1
  echo "Target: $target | host: $host_os $host_arch | Maven profile: $profile"
  echo "JDK path: $JAVA_HOME | JDK version: $jdk_version | JDK architecture: $jdk_arch"
  "$JAVA_HOME/bin/java" -version
  export TMPDIR="$project_dir/../.tools/tmp"
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Duser.home=$project_dir/../.tools/build-home -Djava.io.tmpdir=$TMPDIR"
  version=$(python3 scripts/metadata.py version)
  stem="InventorySystem-$version-$target"
}

build_input() {
  # No Maven clean of the shared target/: other targets' work is preserved.
  python3 scripts/package_target.py prepare "$target" "$build"
  python3 scripts/package_target.py prepare "$target" "$stage"
  ./mvnw -P"$profile" -Djavafx.platform="$native_classifier" -Dinventory.build.directory="$build" package dependency:copy-dependencies \
    -DincludeScope=runtime -DoutputDirectory="$build/input/lib"
  cp "$build/inventory-system-$version.jar" "$build/input/"
  python3 scripts/package_target.py natives "$target" "$build/input"
}
