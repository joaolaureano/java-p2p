#!/usr/bin/env bash
# Starts an interactive peer attached to a super-node.
# Usage: scripts/start-peer.sh <nickname> <peer_port> [server_port=9000] [server_host=127.0.0.1]
set -euo pipefail

cd "$(dirname "$0")/.."

if [ $# -lt 2 ]; then
    echo "Usage: scripts/start-peer.sh <nickname> <peer_port> [server_port=9000] [server_host=127.0.0.1]"
    exit 1
fi

if [ ! -d target/classes ]; then
    echo "Compiling (mvn -q compile)..."
    mvn -q compile
fi

exec java -cp target/classes p2p.peer.PeerNode "${4:-127.0.0.1}" "${3:-9000}" "$1" "$2"
