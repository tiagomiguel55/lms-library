#!/usr/bin/env bash
set -e

if [[ -n "$WAIT_FOR_HOSTS" ]]; then
  IFS=',' read -ra HOSTS <<< "$WAIT_FOR_HOSTS"
  for HP in "${HOSTS[@]}"; do
    HOST="${HP%%:*}"
    PORT="${HP##*:}"
    echo "Waiting for $HOST:$PORT..."
    for i in {1..60}; do
      if bash -c ">/dev/tcp/$HOST/$PORT" 2>/dev/null; then
        echo "  $HOST:$PORT is up!"
        break
      fi
      sleep 2
    done
    if ! bash -c ">/dev/tcp/$HOST/$PORT" 2>/dev/null; then
      echo "ERROR: $HOST:$PORT not reachable" >&2
      exit 1
    fi
  done
fi

echo "Starting application..."
echo "SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE"
exec java -jar /app/app.jar
