#!/usr/bin/env bash
# Runs the tests, then the demo. Needs Python 3.9+ and nothing else.
set -euo pipefail
cd "$(dirname "$0")"
echo "== Tests =="
python3 -m unittest discover -v -s tests -t .
echo
echo "== Demo =="
python3 demo.py
