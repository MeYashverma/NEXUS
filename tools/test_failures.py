#!/usr/bin/env python3
"""Summarize Gradle JUnit XML results for the CI failure log.

Usage: python3 tools/test_failures.py [results-glob]

Prints one block per failing test: class.name, the failure message and the
first app.elevon stack frames. Exit code is always 0 - this is a reporter,
not a gate.
"""
import glob
import html
import re
import sys

pattern = sys.argv[1] if len(sys.argv) > 1 else "app/app/build/test-results/testDebugUnitTest/*.xml"
files = sorted(glob.glob(pattern))
if not files:
    print("(no test-result XMLs found)")
    sys.exit(0)

total_failures = 0
for f in files:
    with open(f, encoding="utf-8", errors="replace") as fh:
        s = fh.read()
    suite = re.search(r'<testsuite[^>]*name="([^"]*)"[^>]*failures="(\d+)"[^>]*errors="(\d+)"', s)
    if suite:
        print(f"## {suite.group(1)}: failures={suite.group(2)} errors={suite.group(3)}")
    case_pat = re.compile(r'<testcase\s+name="([^"]+)"\s+classname="([^"]+)"[^>]*>(.*?)</testcase>', re.S)
    for m in case_pat.finditer(s):
        name, cls, body = m.group(1), m.group(2), m.group(3)
        fm = re.search(r'<(failure|error)\b[^>]*message="([^"]*)"[^>]*>(.*?)</\1>', body, re.S)
        if not fm:
            continue
        total_failures += 1
        kind, msg, stack = fm.group(1), html.unescape(fm.group(2)), html.unescape(fm.group(3))
        print(f"- {cls}.{name} [{kind}]")
        print(f"  {msg[:500]}")
        frames = [line.strip() for line in stack.strip().splitlines() if "app.elevon" in line]
        for line in frames[:5]:
            print(f"  {line}")

if total_failures == 0 and files:
    print("(XMLs present but no <failure> blocks - check the gradle log tail)")
