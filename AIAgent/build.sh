#!/bin/bash
# Build script for Surakarta AI Agent
# Requires Java 11+

set -e

echo "=== Building Surakarta AI Agent ==="

mkdir -p bin

# Compile all sources; data/ is added to classpath so the opening book
# and weights file are found at runtime.
javac -d bin src/*.java

echo "=== Build successful ==="
echo ""
echo "Run with:"
echo "  java -cp bin:data Main"
