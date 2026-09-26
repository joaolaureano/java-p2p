#!/usr/bin/env bash
# Stops the super-nodes started by scripts/start-ring.sh.
set -euo pipefail

cd "$(dirname "$0")/.."
PID_FILE="logs/ring.pids"

if [ ! -f "$PID_FILE" ]; then
    echo "No ring is running ($PID_FILE not found)."
    exit 0
fi

while read -r pid; do
    kill "$pid" 2>/dev/null && echo "stopped super-node (pid $pid)" || true
done < "$PID_FILE"
rm -f "$PID_FILE"
