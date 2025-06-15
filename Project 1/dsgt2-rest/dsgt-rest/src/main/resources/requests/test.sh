#!/bin/bash

# Parse command line arguments
HOST="${1:-localhost}"
PORT="${2:-8080}"

# Configuration
REQUESTS_PER_TYPE=10
MAX_PARALLEL=20
DELAY=0.1

# Create temp directory for results
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

# Function to execute a curl command and track success/failure
execute_curl() {
  local name="$1"
  local cmd="$2"

  # Execute command and capture status code
  status_code=$(eval "$cmd -s -o /dev/null -w '%{http_code}' 2>/dev/null")

  # Record result based on status code
  if [[ "$status_code" =~ ^2[0-9][0-9]$ ]]; then
    echo "success" > "$TEMP_DIR/${name}_$RANDOM"
  else
    echo "failure" > "$TEMP_DIR/${name}_$RANDOM"
  fi
}

# Run all commands in parallel with control
echo "Starting test with $REQUESTS_PER_TYPE requests per endpoint..."
echo "Using host: $HOST:$PORT"

running=0
for i in $(seq 1 $REQUESTS_PER_TYPE); do
  # REST endpoints
  execute_curl "new-meal-rest" "curl -X POST $HOST:$PORT/rest/meals -H 'Content-type:application/json' -d @new-meal.json" &
  ((running++))

  execute_curl "update-meal-rest" "curl -X PUT $HOST:$PORT/rest/meals/cfd1601f-29a0-485d-8d21-7607ec0340c8 -H 'Content-type:application/json' -d @update-meal.json" &
  ((running++))

  execute_curl "delete-meal-rest" "curl -X DELETE $HOST:$PORT/rest/meals/cfd1601f-29a0-485d-8d21-7607ec0340c8 -H 'Content-type:application/json'" &
  ((running++))

  execute_curl "order-rest" "curl -X POST $HOST:$PORT/rest/order -H 'Content-type:application/json' -d @order.json" &
  ((running++))

  # REST-RPC endpoints
  execute_curl "new-meal-restrpc" "curl -X POST $HOST:$PORT/restrpc/meals -H 'Content-type:application/json' -d @new-meal.json" &
  ((running++))

  execute_curl "update-meal-restrpc" "curl -X PUT $HOST:$PORT/restrpc/meals/cfd1601f-29a0-485d-8d21-7607ec0340c8 -H 'Content-type:application/json' -d @update-meal.json" &
  ((running++))

  execute_curl "delete-meal-restrpc" "curl -X DELETE $HOST:$PORT/restrpc/meals/cfd1601f-29a0-485d-8d21-7607ec0340c8 -H 'Content-type:application/json'" &
  ((running++))

  execute_curl "order-restrpc" "curl -X POST $HOST:$PORT/restrpc/order -H 'Content-type:application/json' -d @order.json" &
  ((running++))

  # Control parallel execution
  if [ $running -ge $MAX_PARALLEL ]; then
    wait -n
    ((running--))
  fi

  sleep $DELAY
done

# Wait for all remaining processes to finish
wait

# Count results from temp files
declare -A success_count
declare -A failure_count

for type in "new-meal-rest" "update-meal-rest" "delete-meal-rest" "order-rest" \
           "new-meal-restrpc" "update-meal-restrpc" "delete-meal-restrpc" "order-restrpc"; do
  success_count[$type]=0
  failure_count[$type]=0
done

# Process result files
for result_file in "$TEMP_DIR"/*; do
  name=${result_file##*/}    # Get filename without path
  type=${name%_*}            # Extract type by removing _random suffix
  result=$(cat "$result_file")

  if [ "$result" = "success" ]; then
    ((success_count[$type]++))
  else
    ((failure_count[$type]++))
  fi
done

# Report results
echo "--- Test Results ---"
echo "Total requests: $((REQUESTS_PER_TYPE * 8))"
echo ""
echo "Success rates:"
for type in "${!success_count[@]}"; do
  total=$((success_count[$type] + failure_count[$type]))
  if [ $total -gt 0 ]; then
    success_rate=$((100 * success_count[$type] / total))
    echo "$type: $success_rate% (${success_count[$type]}/$total successful)"
  fi
done