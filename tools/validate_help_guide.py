#!/usr/bin/env python3
"""Validate generated offline Help links, images, fragments, and app destinations."""

from __future__ import annotations

import argparse
from html.parser import HTMLParser
from pathlib import Path
import re
from urllib.parse import unquote, urlsplit
from zipfile import BadZipFile, ZipFile


TOPIC_LINK_PATTERN = re.compile(r"(?:openHelp|newInstance)\(\s*\"([a-z0-9-]+)\"")


class PageReferences(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.links: list[str] = []
        self.images: list[str] = []
        self.fragments: set[str] = set()

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attributes = dict(attrs)
        if tag == "a" and attributes.get("href"):
            self.links.append(attributes["href"] or "")
        if tag == "img" and attributes.get("src"):
            self.images.append(attributes["src"] or "")
        if attributes.get("id"):
            self.fragments.add(attributes["id"] or "")
        if tag == "a" and attributes.get("name"):
            self.fragments.add(attributes["name"] or "")


def _local_target(page: Path, url: str, guide_root: Path) -> tuple[Path, str] | None:
    parsed = urlsplit(url)
    if parsed.scheme or parsed.netloc:
        return None

    relative_path = unquote(parsed.path)
    target = (
        (page.parent / relative_path).resolve() if relative_path else page.resolve()
    )
    root = guide_root.resolve()
    if target != root and root not in target.parents:
        return target, "__outside_guide_root__"
    return target, unquote(parsed.fragment)


def _parse_pages(pages: list[Path]) -> dict[Path, PageReferences]:
    parsed_pages = {}
    for page in pages:
        references = PageReferences()
        references.feed(page.read_text(encoding="utf-8"))
        parsed_pages[page.resolve()] = references
    return parsed_pages


def _validate_image(page: Path, image: str, root: Path) -> list[str]:
    page_name = page.relative_to(root)
    target = _local_target(page, image, root)
    if target is None:
        return [f"{page_name}: image must be bundled locally: {image}"]
    image_path, reason = target
    if reason:
        return [f"{page_name}: image escapes the guide directory: {image}"]
    if not image_path.is_file():
        return [f"{page_name}: missing image {image}"]
    return []


def _validate_link(
    page: Path,
    link: str,
    root: Path,
    parsed_pages: dict[Path, PageReferences],
) -> list[str]:
    target = _local_target(page, link, root)
    if target is None:
        return []
    target_page, fragment = target
    page_name = page.relative_to(root)
    if fragment == "__outside_guide_root__":
        return [f"{page_name}: link escapes the guide directory: {link}"]
    if not target_page.is_file():
        return [f"{page_name}: broken local link {link} (target does not exist)"]
    if not fragment:
        return []

    target_references = parsed_pages.get(target_page.resolve())
    if target_references is None:
        return [
            f"{page_name}: fragment link {link} does not target a generated HTML page"
        ]
    if fragment not in target_references.fragments:
        return [
            f"{page_name}: missing fragment #{fragment} in {target_page.relative_to(root)}"
        ]
    return []


def validate_generated_guide(guide_directory: Path) -> list[str]:
    """Return actionable errors for broken local links, images, and fragment targets."""
    root = guide_directory.resolve()
    pages = sorted(root.rglob("*.html"))
    errors = []
    if not (root / "index.html").is_file():
        errors.append(f"{root}: missing Help contents page index.html")
    if not pages:
        return errors or [f"{root}: no generated HTML pages found"]

    parsed_pages = _parse_pages(pages)
    for page, references in parsed_pages.items():
        for image in references.images:
            errors.extend(_validate_image(page, image, root))
        for link in references.links:
            errors.extend(_validate_link(page, link, root, parsed_pages))
    return errors


def find_app_linked_destinations(source_directories: list[Path]) -> set[str]:
    """Find topic IDs passed as string literals to app Help entry points."""
    destinations: set[str] = set()
    for source_directory in source_directories:
        if not source_directory.exists():
            continue
        for source_file in source_directory.rglob("*.kt"):
            destinations.update(
                TOPIC_LINK_PATTERN.findall(source_file.read_text(encoding="utf-8"))
            )
    return destinations


def validate_app_destinations(
    guide_directory: Path, destinations: set[str]
) -> list[str]:
    errors = []
    for destination in sorted(destinations):
        if not (guide_directory / f"{destination}.html").is_file():
            errors.append(
                f"app links to missing Help destination '{destination}' ({destination}.html)"
            )
    return errors


def validate_packaged_guide(guide_directory: Path, apk_path: Path) -> list[str]:
    """Ensure every generated guide asset is present in the APK's assets directory."""
    errors = []
    try:
        with ZipFile(apk_path) as apk:
            packaged_files = set(apk.namelist())
    except (BadZipFile, OSError) as exception:
        return [f"cannot read APK {apk_path}: {exception}"]

    for asset in sorted(path for path in guide_directory.rglob("*") if path.is_file()):
        asset_path = asset.relative_to(guide_directory).as_posix()
        packaged_path = f"assets/{asset_path}"
        if packaged_path not in packaged_files:
            errors.append(
                f"APK {apk_path}: missing bundled Help asset '{packaged_path}'"
            )
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--generated",
        action="append",
        type=Path,
        required=True,
        help="generated guide asset directory (repeatable)",
    )
    parser.add_argument(
        "--source",
        action="append",
        type=Path,
        default=[],
        help="production Kotlin source directory (repeatable)",
    )
    parser.add_argument(
        "--apk",
        action="append",
        type=Path,
        required=True,
        help="debug APK to inspect for bundled Help assets (repeatable)",
    )
    args = parser.parse_args()

    linked_destinations = find_app_linked_destinations(args.source)
    errors = []
    for guide_directory in args.generated:
        errors.extend(
            f"{guide_directory}: {error}"
            for error in validate_generated_guide(guide_directory)
        )
        errors.extend(
            f"{guide_directory}: {error}"
            for error in validate_app_destinations(guide_directory, linked_destinations)
        )
    for apk in args.apk:
        errors.extend(validate_packaged_guide(args.generated[0], apk))

    if errors:
        print("Help guide validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print(
        f"Help guide validation passed for {len(args.generated)} generated guide(s); app destinations: {', '.join(sorted(linked_destinations))}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
