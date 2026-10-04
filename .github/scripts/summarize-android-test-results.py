#!/usr/bin/env python3
"""Print a compact per-test summary from Android Gradle Plugin XML reports."""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def main() -> None:
    results_dir = Path(sys.argv[1])
    reports = sorted(results_dir.rglob("TEST-*.xml"))
    test_cases = []

    for report in reports:
        try:
            root = ET.parse(report).getroot()
        except ET.ParseError as error:
            print(f"Could not parse test report {report}: {error}", file=sys.stderr)
            continue
        test_cases.extend(root.iter("testcase"))

    if not test_cases:
        print("No instrumented test result XML reports found.")
        return

    counts = {"passed": 0, "failed": 0, "errors": 0, "skipped": 0}
    failures = []
    for case in test_cases:
        issue = case.find("failure")
        error = case.find("error")
        skipped = case.find("skipped")
        if issue is not None:
            status = "FAIL"
            counts["failed"] += 1
            failures.append((case, issue))
        elif error is not None:
            status = "ERROR"
            counts["errors"] += 1
            failures.append((case, error))
        elif skipped is not None:
            status = "SKIP"
            counts["skipped"] += 1
        else:
            status = "PASS"
            counts["passed"] += 1

        classname = case.get("classname", "")
        name = case.get("name", "unknown test")
        duration = case.get("time")
        elapsed = f" ({duration}s)" if duration else ""
        print(f"{status} {classname}#{name}{elapsed}")

    print(
        "Instrumented test results: "
        f"{counts['passed']} passed, {counts['failed']} failed, "
        f"{counts['errors']} errors, {counts['skipped']} skipped"
    )

    for case, detail in failures:
        print(f"\n{case.get('classname', '')}#{case.get('name', 'unknown test')}: ")
        message = detail.get("message", "").strip()
        if message:
            print(f"  {message}")
        trace = (detail.text or "").strip().splitlines()
        for line in trace[:12]:
            print(f"  {line.strip()}")
        if len(trace) > 12:
            print(f"  ... {len(trace) - 12} more lines (see uploaded test report)")


if __name__ == "__main__":
    main()
