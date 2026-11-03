#!/bin/sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -z "${JAVA_HOME:-}" ] && [ -d "$SCRIPT_DIR/.tools/jdk/Contents/Home" ]; then
  JAVA_HOME="$SCRIPT_DIR/.tools/jdk/Contents/Home"
  export JAVA_HOME
fi
cd "$SCRIPT_DIR/InventorySystemi"
exec ./mvnw "$@"
