#!/bin/sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -z "${JAVA_HOME:-}" ] && [ -d "$SCRIPT_DIR/.tools/jdk/Contents/Home" ]; then
  JAVA_HOME="$SCRIPT_DIR/.tools/jdk/Contents/Home"
  export JAVA_HOME
fi
mkdir -p "$SCRIPT_DIR/.tools/tmp"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djava.io.tmpdir=$SCRIPT_DIR/.tools/tmp"
cd "$SCRIPT_DIR/InventorySystemi"
exec ./mvnw "$@"
