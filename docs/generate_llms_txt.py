#!/usr/bin/env python3
"""Generate llms.txt from Smithy RST documentation files.

Walks source-2.0/, extracts titles from RST files, maps them to the raw
reStructuredText source on GitHub, and writes a structured llms.txt
suitable for indexing by the AWS Knowledge MCP.

Links point at the .rst source rather than the rendered .html: the .rst
is the complete, version-matched text, whereas an agent's web-fetch of
the rendered HTML is lossy when it summarizes. This makes llms.txt a
single index that doubles as the map the docs-navigator skill reads.
"""

import json
import os
import re
import sys

RAW_BASE = "https://raw.githubusercontent.com/smithy-lang/smithy/main/docs/source-2.0"
SOURCE_DIR = "source-2.0"

# Sections in display order. Keys are directory prefixes relative to SOURCE_DIR;
# None means top-level files. Values are section headings for llms.txt.
SECTIONS = [
    (None, None),  # top-level pages (quickstart, index, trait-index)
    ("spec", "Specification"),
    ("guides", "Guides"),
    ("tutorials", "Tutorials"),
    ("additional-specs", "Additional Specs"),
    ("aws", "AWS Integrations"),
    ("languages", "Languages"),
]

RST_TITLE_RE = re.compile(r"^([=\-~`#\"^+*:.!'_]{2,})\s*$")


def extract_title(filepath):
    """Extract the first RST heading from a file."""
    with open(filepath, encoding="utf-8") as f:
        lines = f.readlines()

    for i, line in enumerate(lines):
        stripped = line.rstrip()
        if not RST_TITLE_RE.match(stripped):
            continue
        # Overline+title+underline pattern
        if i + 2 < len(lines) and RST_TITLE_RE.match(lines[i + 2].rstrip()):
            return lines[i + 1].strip()
        # Title+underline pattern (current line is underline, previous is title)
        if i > 0 and lines[i - 1].strip():
            return lines[i - 1].strip()
    return None


def rst_path_to_url(rst_path):
    """Convert a source-relative RST path to its raw .rst source URL.

    The .rst source is the complete, version-matched text; the rendered
    .html page is lossy when an agent's web-fetch summarizes it.
    """
    rel = rst_path.replace(os.sep, "/")
    return f"{RAW_BASE}/{rel}"


# Pages whose .rst source is a build-time directive stub (no readable content),
# so the curated overlay links their rendered .html instead. Skip them in the walk.
EXCLUDE_PAGES = {"trait-index.rst"}


def collect_pages(source_dir):
    """Walk source_dir and return {relative_path: title} for all RST files."""
    pages = {}
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
                pages[rel] = title
    return pages


def classify(rel_path):
    """Return the section prefix for a relative path, or None for top-level.

    Paths are already normalized to forward slashes by collect_pages().
    """
    for prefix, _ in SECTIONS:
        if prefix and rel_path.startswith(prefix + "/"):
            return prefix
    return None


def load_overlay(docs_dir):
    """Load the curated overlay (extra sections) if present, else None."""
    path = os.path.join(docs_dir, "llms_extra.json")
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def render_overlay_sections(overlay):
    """Render curated overlay sections as Markdown lines."""
    lines = []
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


def generate(source_dir, output_path, overlay=None):
    pages = collect_pages(source_dir)

    lines = [
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
            p: t for p, t in pages.items() if classify(p) == prefix
        }
        if not section_pages:
            continue

        if heading:
            lines.append(f"## {heading}")
            lines.append("")

        for rel_path in sorted(section_pages):
            title = section_pages[rel_path]
            url = rst_path_to_url(rel_path)
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
