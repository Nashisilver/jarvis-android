#!/bin/sh
APP_HOME="$(dirname "$0")"
exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || \
  gradle "$@"
