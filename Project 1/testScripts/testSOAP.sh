#!/bin/bash

HOST="${1:-localhost}"
PORT="${2:-8080}"

echo "Starting SOAP wrk stress test on $HOST:$PORT"

wrk -t12 -c400 -d120s --latency -s testSOAP.lua http://$HOST:$PORT

