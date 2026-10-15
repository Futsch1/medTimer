import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from validate_help_guide import (
    find_app_linked_destinations,
    validate_app_destinations,
    validate_generated_guide,
    validate_packaged_guide,
)


class ValidateHelpGuideTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary_directory.name)
        (self.root / "index.html").write_text(
            '<h1 id="contents">Help</h1><a href="topic.html#details">Topic</a>'
        )
        (self.root / "topic.html").write_text(
            '<h1 id="details">Topic</h1><img src="images/example.png" alt="Example">'
        )
        (self.root / "images").mkdir()
        (self.root / "images/example.png").write_bytes(b"synthetic image")

    def tearDown(self) -> None:
        self.temporary_directory.cleanup()

    def test_accepts_valid_links_images_and_fragment_targets(self) -> None:
        self.assertEqual([], validate_generated_guide(self.root))

    def test_reports_broken_local_topic_link(self) -> None:
        (self.root / "index.html").write_text(
            '<a href="missing-topic.html">Missing topic</a>'
        )

        errors = validate_generated_guide(self.root)

        self.assertTrue(
            any("broken local link missing-topic.html" in error for error in errors),
            errors,
        )

    def test_reports_missing_fragment_target(self) -> None:
        (self.root / "index.html").write_text(
            '<a href="topic.html#not-there">Missing section</a>'
        )

        errors = validate_generated_guide(self.root)

        self.assertTrue(
            any("missing fragment #not-there" in error for error in errors), errors
        )

    def test_reports_missing_image(self) -> None:
        (self.root / "topic.html").write_text(
            '<img src="images/absent.png" alt="Missing">'
        )

        errors = validate_generated_guide(self.root)

        self.assertTrue(
            any("missing image images/absent.png" in error for error in errors), errors
        )

    def test_reports_app_link_to_missing_destination(self) -> None:
        errors = validate_app_destinations(self.root, {"missing-topic"})

        self.assertEqual(
            [
                "app links to missing Help destination 'missing-topic' (missing-topic.html)"
            ],
            errors,
        )

    def test_reports_missing_asset_in_apk(self) -> None:
        apk = self.root.with_suffix(".apk")
        with ZipFile(apk, "w") as package:
            package.writestr("assets/index.html", "contents")

        errors = validate_packaged_guide(self.root, apk)

        self.assertTrue(
            any(
                "missing bundled Help asset 'assets/topic.html'" in error
                for error in errors
            ),
            errors,
        )

    def test_accepts_generated_assets_in_apk(self) -> None:
        apk = self.root.with_suffix(".apk")
        with ZipFile(apk, "w") as package:
            for asset in self.root.rglob("*"):
                if asset.is_file() and asset != apk:
                    package.write(
                        asset, f"assets/{asset.relative_to(self.root).as_posix()}"
                    )

        self.assertEqual([], validate_packaged_guide(self.root, apk))

    def test_finds_literal_destinations_in_app_sources(self) -> None:
        source = self.root / "src"
        source.mkdir()
        (source / "Entry.kt").write_text(
            'actions.openHelp("getting-started")\nHelpReaderDialogFragment.newInstance("reminder-types")'
        )

        self.assertEqual(
            {"getting-started", "reminder-types"},
            find_app_linked_destinations([source]),
        )


if __name__ == "__main__":
    unittest.main()
