#!/bin/bash

HOST="${1:-localhost}"
PORT="${2:-8080}"

echo "Starting wrk stress test on $HOST:$PORT"

wrk -t12 -c400 -d30s --latency -s testREST.lua http://$HOST:$PORT

