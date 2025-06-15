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

# Function to execute a SOAP request and track success/failure
execute_soap() {
  local name="$1"
  local xml_file="$2"

  # Execute command and capture status code
  status_code=$(curl --silent --header "content-type: text/xml" \
                -d @"$xml_file" \
                -o /dev/null \
                -w '%{http_code}' \
                "http://$HOST:$PORT/ws" 2>/dev/null)

  # Record result based on status code
  if [[ "$status_code" =~ ^2[0-9][0-9]$ ]]; then
    echo "success" > "$TEMP_DIR/${name}_$RANDOM"
  else
    echo "failure" > "$TEMP_DIR/${name}_$RANDOM"
  fi
}

# Run all commands in parallel with control
echo "Starting SOAP test with $REQUESTS_PER_TYPE requests per endpoint..."
echo "Using host: $HOST:$PORT"

running=0
for i in $(seq 1 $REQUESTS_PER_TYPE); do
  # SOAP endpoints
  execute_soap "get-meal" "request.xml" &
  ((running++))

  execute_soap "get-largest-meal" "requestbiggest.xml" &
  ((running++))

  execute_soap "get-cheapest-meal" "requestcheapest.xml" &
  ((running++))

  execute_soap "add-order" "requestorder.xml" &
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

for type in "get-meal" "get-largest-meal" "get-cheapest-meal" "add-order"; do
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
echo "Total requests: $((REQUESTS_PER_TYPE * 4))"
echo ""
echo "Success rates:"
for type in "${!success_count[@]}"; do
  total=$((success_count[$type] + failure_count[$type]))
  if [ $total -gt 0 ]; then
    success_rate=$((100 * success_count[$type] / total))
    echo "$type: $success_rate% (${success_count[$type]}/$total successful)"
  fi
done