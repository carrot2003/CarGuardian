#!/usr/bin/env bash

set -euo pipefail

if ! command -v gradle >/dev/null 2>&1; then
  echo "未检测到系统 gradle，请先安装（例如 macOS: brew install gradle）" 1>&2
  exit 1
fi

exec gradle "$@"
