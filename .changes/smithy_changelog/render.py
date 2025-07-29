# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
from argparse import ArgumentParser

from . import Change, Release

try:
    import mdformat
except ImportError as e:
    raise Exception(
        "Required dependency of render not found. Please install the smithy_changelog "
        "package or run render via `uv run render` to ensure the dependency is "
        "available."
    ) from e


_DEFAULT_TITLE = "Smithy Changelog"


def main() -> None:
    parser = ArgumentParser(
        description="Render the changelog as markdown",
    )
    parser.add_argument(
        "-t", "--title", help="The top-level title to use for the changelog file."
    )
    args = parser.parse_args()
    print(render(title=args.title))


def render(title: str | None) -> str:
    title = title or _DEFAULT_TITLE
    rendered = f"# {title}\n\n"
    for release in _get_releases():
        if isinstance(release, str):
            rendered += release + "\n"
            continue

        rendered += f"## {release.version} ({release.date})\n\n"

        for change_type, changes in release.change_map().items():
            rendered += f"### {change_type.section_title}\n\n"

            for change in changes:
                rendered += _render_change(change)

            rendered += "\n"

    rendered = mdformat.text(  # pyright: ignore [reportUnknownMemberType]
        rendered, options={"wrap": 80}
    )
    return rendered.strip() + "\n"


def _render_change(change: Change) -> str:
    lines = change.description.strip().splitlines()
    rendered = f"- {lines[0]}"

    # Indend any additional lines in the description if they have
    # content.
    if len(lines) > 1:
        for line in lines[1:]:
            if not line:
                rendered += "\n"
                continue

            rendered += f"\n  {line}"

    if prs := change.pull_requests:
        rendered += f" ({', '.join(prs)})"
    return rendered + "\n"


def _get_releases() -> list[Release | str]:
    releases: list[Release | str] = []
    for release_file in Release.get_release_files():
        if release_file.name.endswith("md"):
            releases.append(release_file.read_text().strip())
        else:
            releases.append(Release.read(release_file))
    return releases
