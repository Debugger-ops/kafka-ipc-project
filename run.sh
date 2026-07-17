#!/usr/bin/env bash
# Helper script to run the Kafka IPC demo end to end.
# Usage: ./run.sh [up|topic|produce|consume|down]
set -euo pipefail

JAR="target/kafka-ipc.jar"

case "${1:-help}" in
  up)
    echo ">> Starting Zookeeper + Kafka (+ Kafka UI on http://localhost:8080)..."
    docker compose up -d
    echo ">> Waiting for the broker to become healthy..."
    until [ "$(docker inspect -f '{{.State.Health.Status}}' ipc-kafka 2>/dev/null || echo starting)" = "healthy" ]; do
      sleep 2
    done
    echo ">> Kafka is up."
    ;;
  build)
    echo ">> Building the project..."
    mvn -q clean package
    ;;
  topic)
    java -jar "$JAR" create-topic
    ;;
  produce)
    java -jar "$JAR" produce "${2:-20}"
    ;;
  consume)
    java -jar "$JAR" consume
    ;;
  down)
    echo ">> Tearing down containers..."
    docker compose down
    ;;
  *)
    echo "Usage: ./run.sh [up|build|topic|produce|consume|down]"
    echo "  up       start Zookeeper + Kafka via docker compose"
    echo "  build    mvn clean package (creates target/kafka-ipc.jar)"
    echo "  topic    create the ipc-events topic"
    echo "  produce  stream events (optional count, e.g. ./run.sh produce 50)"
    echo "  consume  subscribe and print incoming events"
    echo "  down     stop and remove containers"
    ;;
esac
