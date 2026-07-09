#!/usr/bin/env python3
"""Generate llms.txt from Smithy RST documentation files.

Finds every .rst file under source-2.0/, extracts each page's title, pairs
it with the page's raw reStructuredText source URL on GitHub, and writes a
structured llms.txt suitable for indexing by the AWS Knowledge MCP.

Links point at the .rst source rather than the rendered .html: the .rst
is the complete, version-matched text, whereas an agent's web-fetch of
the rendered HTML is lossy when it summarizes. The result is also the
index the smithy-docs-navigator skill reads.
"""

import json
import os
import re
import sys

RAW_BASE: str = "https://raw.githubusercontent.com/smithy-lang/smithy/main/docs"
SOURCE_DIR: str = "source-2.0"
OVERLAY_FILE: str = "llms_extra.json"

# Sections in display order. Keys are directory prefixes relative to SOURCE_DIR
# (None means top-level files). Values are section headings for llms.txt.
SECTIONS: list[tuple[str | None, str | None]] = [
    (None, None),  # top-level pages (quickstart, index, trait-index)
    ("spec", "Specification"),
    ("guides", "Guides"),
    ("tutorials", "Tutorials"),
    ("additional-specs", "Additional Specs"),
    ("aws", "AWS Integrations"),
    ("languages", "Languages"),
]

RST_TITLE_RE: re.Pattern[str] = re.compile(r"^([=\-~`#\"^+*:.!'_]{2,})\s*$")


def extract_title(filepath: str) -> str | None:
    """Extract the first RST heading from a file."""
    with open(filepath, encoding="utf-8") as f:
        lines = f.readlines()

    for i, line in enumerate(lines):
        if not RST_TITLE_RE.match(line.rstrip()):
            continue
        # Overline+title+underline pattern
        if i + 2 < len(lines) and RST_TITLE_RE.match(lines[i + 2].rstrip()):
            return lines[i + 1].strip()
        # Title+underline pattern (current line is underline, previous is title)
        if i > 0 and lines[i - 1].strip():
            return lines[i - 1].strip()
    return None


def rst_path_to_url(on_disk_path: str, docs_dir: str) -> str:
    """Convert an on-disk RST path to its raw .rst source URL.

    Resolves symlinks first: some pages under source-2.0/ are symlinks into
    source-shared/ (e.g. languages/typescript/ts-ssdk/), and raw.githubusercontent.com
    serves the symlink's target-path string, not the file, so a link built from
    the symlink path 404s. The link must point at the real tracked file.
    """
    real = os.path.realpath(on_disk_path)
    rel = os.path.relpath(real, docs_dir).replace(os.sep, "/")
    return f"{RAW_BASE}/{rel}"


# Pages whose .rst source contains only a build-time directive and no readable
# text. Skip them here, and let llms_extra.json link their rendered .html instead.
EXCLUDE_PAGES: set[str] = {"trait-index.rst"}


def collect_pages(source_dir: str) -> dict[str, tuple[str, str]]:
    """Find every .rst file under source_dir.

    Returns {source-relative path: (title, raw source URL)}. The key stays the
    source-2.0-relative path so section classification works; the URL is built
    from the real (symlink-resolved) file location.
    """
    docs_dir = os.path.dirname(source_dir)
    pages: dict[str, tuple[str, str]] = {}
    for root, _, files in os.walk(source_dir, followlinks=True):
        for fname in sorted(files):
            if not fname.endswith(".rst"):
                continue
            full = os.path.join(root, fname)
            rel = os.path.relpath(full, source_dir).replace(os.sep, "/")
            if rel in EXCLUDE_PAGES:
                continue
            title = extract_title(full)
            if title:
                pages[rel] = (title, rst_path_to_url(full, docs_dir))
    return pages


def classify(rel_path: str) -> str | None:
    """Return the section prefix for a relative path, or None for top-level.

    Paths are already normalized to forward slashes by collect_pages().
    """
    for prefix, _ in SECTIONS:
        if prefix and rel_path.startswith(prefix + "/"):
            return prefix
    return None


def load_overlay(docs_dir: str) -> dict | None:
    """Load the hand-picked extra links (llms_extra.json) if present, else None.

    The file is hand-maintained, so a syntax slip is the likely failure. Fail
    loudly with the file named rather than letting a bare JSON traceback bubble
    up from the middle of the docs build.
    """
    path = os.path.join(docs_dir, OVERLAY_FILE)
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return None
    except json.JSONDecodeError as e:
        sys.exit(f"{path}: invalid JSON ({e})")


def render_overlay_sections(overlay: dict) -> list[str]:
    """Render the extra-links sections as Markdown lines."""
    lines: list[str] = []
    for section in overlay.get("sections", []):
        lines.append(f"## {section['heading']}")
        lines.append("")
        if section.get("note"):
            lines.append(f"> {section['note']}")
            lines.append("")
        for link in section.get("links", []):
            entry = f"- [{link['title']}]({link['url']})"
            if link.get("note"):
                entry += f" - {link['note']}"
            lines.append(entry)
        lines.append("")
    return lines


def generate(source_dir: str, output_path: str, overlay: dict | None = None) -> None:
    pages = collect_pages(source_dir)

    lines: list[str] = [
        "# Smithy",
        "",
        "> Smithy is an interface definition language (IDL) and set of tools"
        " for building clients, servers, and documentation for any programming"
        " language. Smithy models define a service as a collection of resources,"
        " operations, and shapes.",
        "",
        "> Links point at the reStructuredText source (.rst): it is the complete,"
        " version-matched text, whereas a summarized web-fetch of the rendered HTML"
        " drops exact rules. When changing an existing model, consult the Evolving"
        " Models guide under Key references first - it explains which changes are"
        " backward compatible and which break customers.",
        "",
    ]

    for prefix, heading in SECTIONS:
        section_pages = {
            p: entry for p, entry in pages.items() if classify(p) == prefix
        }
        if not section_pages:
            continue

        if heading:
            lines.append(f"## {heading}")
            lines.append("")

        for rel_path in sorted(section_pages):
            title, url = section_pages[rel_path]
            lines.append(f"- [{title}]({url})")

        lines.append("")

    if overlay:
        lines.extend(render_overlay_sections(overlay))

    text = "\n".join(lines)
    os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"Generated {output_path} ({len(pages)} pages)")


if __name__ == "__main__":
    docs_dir = os.path.dirname(os.path.abspath(__file__))
    source = os.path.join(docs_dir, SOURCE_DIR)
    output = sys.argv[1] if len(sys.argv) > 1 else os.path.join(docs_dir, "build", "html", "2.0", "llms.txt")
    generate(source, output, load_overlay(docs_dir))
