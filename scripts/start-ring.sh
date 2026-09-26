#!/usr/bin/env bash
# Starts a ring of N super-nodes on localhost in the background.
# Usage: scripts/start-ring.sh [ring_size=4] [base_port=9000]
set -euo pipefail

cd "$(dirname "$0")/.."
RING_SIZE="${1:-4}"
BASE_PORT="${2:-9000}"
LOG_DIR="logs"

if [ ! -d target/classes ]; then
    echo "Compiling (mvn -q compile)..."
    mvn -q compile
fi

mkdir -p "$LOG_DIR"
: > "$LOG_DIR/ring.pids"

for ((position = 1; position <= RING_SIZE; position++)); do
    port=$((BASE_PORT + position - 1))
    next_port=$((BASE_PORT + position % RING_SIZE))
    java -cp target/classes p2p.server.SuperNode "$port" "$next_port" "$position" "$RING_SIZE" \
        > "$LOG_DIR/node-$port.log" 2>&1 &
    echo $! >> "$LOG_DIR/ring.pids"
    echo "super-node $position/$RING_SIZE on port $port -> next $next_port (log: $LOG_DIR/node-$port.log)"
done

echo
echo "Ring is up. Start a peer with:  scripts/start-peer.sh <nickname> <peer_port> [server_port=$BASE_PORT]"
echo "Stop the ring with:             scripts/stop-ring.sh"
