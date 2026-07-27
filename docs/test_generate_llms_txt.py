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
