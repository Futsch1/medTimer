#!/usr/bin/env python3
"""Print a compact per-test summary from Android Gradle Plugin XML reports."""

import sys
from pathlib import Path

import xml.etree.ElementTree as ET  # nosec B405 - reports are generated locally by Gradle in CI.


def parse_report(report: Path) -> list[ET.Element]:
    try:
        root = ET.parse(report).getroot()  # nosec B314 - report XML is generated locally by Gradle in CI.
    except ET.ParseError as error:
        print(f"Could not parse test report {report}: {error}", file=sys.stderr)
        return []
    return list(root.iter("testcase"))


def load_test_cases(results_dir: Path) -> list[ET.Element]:
    test_cases = []
    for report in sorted(results_dir.rglob("TEST-*.xml")):
        test_cases.extend(parse_report(report))
    return test_cases


def get_status(case: ET.Element) -> tuple[str, ET.Element | None]:
    issue = case.find("failure")
    if issue is not None:
        return "FAIL", issue

    error = case.find("error")
    if error is not None:
        return "ERROR", error

    if case.find("skipped") is not None:
        return "SKIP", None
    return "PASS", None


def print_test_case(
    case: ET.Element,
    counts: dict[str, int],
    failures: list[tuple[ET.Element, ET.Element]],
) -> None:
    status, detail = get_status(case)
    count_key = {
        "PASS": "passed",
        "FAIL": "failed",
        "ERROR": "errors",
        "SKIP": "skipped",
    }[status]
    counts[count_key] += 1

    classname = case.get("classname", "")
    name = case.get("name", "unknown test")
    duration = case.get("time")
    elapsed = f" ({duration}s)" if duration else ""
    print(f"{status} {classname}#{name}{elapsed}")

    if detail is not None:
        failures.append((case, detail))


def print_failure(case: ET.Element, detail: ET.Element) -> None:
    print(f"\n{case.get('classname', '')}#{case.get('name', 'unknown test')}:")
    message = detail.get("message", "").strip()
    if message:
        print(f"  {message}")

    trace = (detail.text or "").strip().splitlines()
    for line in trace[:12]:
        print(f"  {line.strip()}")
    if len(trace) > 12:
        print(f"  ... {len(trace) - 12} more lines (see uploaded test report)")


def print_summary(test_cases: list[ET.Element]) -> None:
    counts = {"passed": 0, "failed": 0, "errors": 0, "skipped": 0}
    failures = []
    for case in test_cases:
        print_test_case(case, counts, failures)

    print(
        "Instrumented test results: "
        f"{counts['passed']} passed, {counts['failed']} failed, "
        f"{counts['errors']} errors, {counts['skipped']} skipped"
    )
    for case, detail in failures:
        print_failure(case, detail)


def main() -> None:
    if len(sys.argv) != 2:
        print(f"Usage: {Path(sys.argv[0]).name} <results-directory>", file=sys.stderr)
        raise SystemExit(2)

    test_cases = load_test_cases(Path(sys.argv[1]))
    if not test_cases:
        print("No instrumented test result XML reports found.")
        return
    print_summary(test_cases)


if __name__ == "__main__":
    main()
