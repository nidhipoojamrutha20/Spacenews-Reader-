#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "Checking Android SDK configuration..."

if [[ ! -f local.properties && -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "Android SDK not configured."
  echo "Open the project in Android Studio or set sdk.dir in local.properties."
  exit 1
fi

echo "Building SpaceNews..."
./gradlew :app:assembleDebug

echo "Running unit tests..."
./gradlew :app:testDebugUnitTest

echo "Running Android Lint..."
./gradlew :app:lintDebug

echo "All build, unit-test and lint tasks passed."
