import tempfile
import unittest
from pathlib import Path

import generate_llms_txt


DOCS_DIR = Path(__file__).parent
CLIENT_GUIDANCE_PATH = "guides/client-guidance/index.md"
CLIENT_GUIDANCE_URL = (
    "https://raw.githubusercontent.com/smithy-lang/smithy/main/"
    "docs/source-2.0/guides/client-guidance/index.md"
)


class GenerateLlmsTxtTest(unittest.TestCase):
    def test_collect_pages_includes_markdown_sources(self) -> None:
        pages = generate_llms_txt.collect_pages(
            str(DOCS_DIR / generate_llms_txt.SOURCE_DIR)
        )

        self.assertEqual(
            ("Smithy Client Guidance", CLIENT_GUIDANCE_URL),
            pages[CLIENT_GUIDANCE_PATH],
        )

    def test_collect_pages_includes_template_sources(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            source_dir = Path(directory) / "source-2.0"
            source_dir.mkdir()
            (source_dir / "example.rst.template").write_text(
                "=================\nRST template page\n=================\n",
                encoding="utf-8",
            )
            (source_dir / "example.md.template").write_text(
                "# Markdown template page\n",
                encoding="utf-8",
            )

            pages = generate_llms_txt.collect_pages(str(source_dir))

        self.assertEqual("RST template page", pages["example.rst.template"][0])
        self.assertEqual("Markdown template page", pages["example.md.template"][0])

    def test_collect_pages_extracts_clean_title_from_partial_with_substitutions(
        self,
    ) -> None:
        # A real .rst.template partial leads with a section heading but its body carries
        # unresolved substitutions (|quoted shape name|) that only resolve in the parent
        # page. The indexed title must still be the clean heading, not the raw markup.
        with tempfile.TemporaryDirectory() as directory:
            source_dir = Path(directory) / "source-2.0"
            source_dir.mkdir()
            (source_dir / "supported-traits.rst.template").write_text(
                "----------------\nSupported traits\n----------------\n\n"
                "The |quoted shape name| protocol supports the following traits.\n",
                encoding="utf-8",
            )

            pages = generate_llms_txt.collect_pages(str(source_dir))

        self.assertEqual(
            "Supported traits", pages["supported-traits.rst.template"][0]
        )

    def test_collect_pages_skips_partial_without_heading(self) -> None:
        # A partial that opens with body content instead of a heading (e.g. a
        # `.. list-table::` directive or an enumerated list) has no title to link, so it
        # is intentionally left out of the index rather than emitted with a bogus title.
        with tempfile.TemporaryDirectory() as directory:
            source_dir = Path(directory) / "source-2.0"
            source_dir.mkdir()
            (source_dir / "headingless.rst.template").write_text(
                ".. list-table::\n    :header-rows: 1\n\n    * - Trait\n      - Description\n",
                encoding="utf-8",
            )

            pages = generate_llms_txt.collect_pages(str(source_dir))

        self.assertNotIn("headingless.rst.template", pages)

    def test_generate_includes_markdown_source_link(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output_path = Path(directory) / "llms.txt"
            generate_llms_txt.generate(
                str(DOCS_DIR / generate_llms_txt.SOURCE_DIR),
                str(output_path),
            )

            output = output_path.read_text(encoding="utf-8")

        self.assertIn(
            f"[Smithy Client Guidance]({CLIENT_GUIDANCE_URL})",
            output,
        )
