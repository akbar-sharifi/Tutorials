#!/usr/bin/env bash
# Compiles everything with plain javac (Java 17+), runs the tests, then the demo.
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
javac -d out $(find src -name '*.java')
echo "== Tests =="
java -cp out com.tutorials.circuitbreaker.CircuitBreakerTest
echo
echo "== Demo =="
java -cp out com.tutorials.circuitbreaker.demo.CircuitBreakerDemo
