#!/bin/bash
set -e

echo "Building all modules..."
mvn clean install -DskipTests -B -ntp -q

echo "Running all tests..."
mvn test -B -ntp -q

echo ""
echo "✓ All tests passed!"
