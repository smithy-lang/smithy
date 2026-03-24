#!/usr/bin/env python3
"""Generate llms.txt from Smithy RST documentation files.

Walks source-2.0/, extracts titles from RST files, maps them to
smithy.io URLs, and writes a structured llms.txt suitable for
indexing by the AWS Knowledge MCP.
"""

import os
import re
import sys

BASE_URL = "https://smithy.io/2.0"
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
    """Convert a source-relative RST path to a smithy.io URL."""
    rel = rst_path.replace(os.sep, "/")
    html = rel.removesuffix(".rst") + ".html"
    return f"{BASE_URL}/{html}"


def collect_pages(source_dir):
    """Walk source_dir and return {relative_path: title} for all RST files."""
    pages = {}
    for root, _, files in os.walk(source_dir, followlinks=True):
        for fname in sorted(files):
            if not fname.endswith(".rst"):
                continue
            full = os.path.join(root, fname)
            rel = os.path.relpath(full, source_dir)
            title = extract_title(full)
            if title:
                pages[rel] = title
    return pages


def classify(rel_path):
    """Return the section prefix for a relative path, or None for top-level."""
    first = rel_path.split(os.sep)[0] if os.sep in rel_path else rel_path.split("/")[0]
    for prefix, _ in SECTIONS:
        if prefix and rel_path.startswith(prefix + "/"):
            return prefix
    return None


def generate(source_dir, output_path):
    pages = collect_pages(source_dir)

    lines = [
        "# Smithy",
        "",
        "> Smithy is an interface definition language (IDL) and set of tools"
        " for building clients, servers, and documentation for any programming"
        " language. Smithy models define a service as a collection of resources,"
        " operations, and shapes.",
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

    text = "\n".join(lines)
    os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"Generated {output_path} ({len(pages)} pages)")


if __name__ == "__main__":
    docs_dir = os.path.dirname(os.path.abspath(__file__))
    source = os.path.join(docs_dir, SOURCE_DIR)
    output = sys.argv[1] if len(sys.argv) > 1 else os.path.join(docs_dir, "build", "html", "2.0", "llms.txt")
    generate(source, output)
