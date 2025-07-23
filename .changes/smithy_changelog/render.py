# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
from argparse import ArgumentParser
from dataclasses import dataclass
from pathlib import Path
from typing import Self

from . import RELEASES_DIR, Change, Release
from .release import release as _release

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
        description="""\
            Render the changelog as markdown, optionally including pending features \
            as a new release.""",
    )
    parser.add_argument(
        "-t",
        "--title",
        help="The top-level title to use for the changelog file.",
        default=_DEFAULT_TITLE,
    )
    release_group = parser.add_argument_group(
        "release",
        description="""\
            These arguments allow for releasing all pending features in the \
            next-release folder as a new release. If not set, the exisiting releases \
            will be re-rendered.""",
    )
    release_group.add_argument(
        "-v",
        "--release-version",
        type=str,
        help="""\
            The version to use for the staged changelog release. If set, all pending \
            features will be compiled into a release.""",
    )
    release_group.add_argument(
        "-d",
        "--release-date",
        type=str,
        help="""\
            The date of the release in ISO format (e.g. 2024-11-13). If not set, \
            today's date, according to your local time zone, will be used.""",
    )

    args = parser.parse_args()

    if args.release_version:
        _release(args.release_version, args.release_date)

    render(title=args.title)


def render(title: str = _DEFAULT_TITLE) -> None:
    rendered = f"# {title}\n\n"
    for release in get_releases():
        if isinstance(release, str):
            rendered += release + "\n"
            continue

        rendered += f"## {release.version} ({release.date})\n\n"

        for change_type, changes in release.change_map().items():
            rendered += f"### {change_type.section_title}\n\n"

            for change in changes:
                rendered += render_change(change)

            rendered += "\n"

    rendered = mdformat.text(  # pyright: ignore [reportUnknownMemberType]
        rendered, options={"wrap": 80}
    )
    rendered = rendered.strip() + "\n"
    print(rendered)


def render_change(change: Change) -> str:
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


def get_releases() -> list[Release | str]:
    releases: list[Release | str] = []
    release_files = list(RELEASES_DIR.glob("*.json"))
    release_files.extend(RELEASES_DIR.glob("*.md"))
    release_files = sorted(
        release_files, key=lambda p: Version.from_path(p), reverse=True
    )
    for release_file in release_files:
        if release_file.name.endswith("md"):
            releases.append(release_file.read_text().strip())
        else:
            releases.append(Release.read(release_file))
    return releases


# This exists to allow for sorting the release files based on the version in
# the file name.
@dataclass
class Version:
    major: int
    minor: int
    patch: int

    def __lt__(self, other: Self) -> bool:
        if self.major != other.major:
            return self.major < other.major
        if self.minor != other.minor:
            return self.minor < other.minor
        return self.patch < other.patch

    @classmethod
    def from_path(cls, path: Path) -> Self:
        parts = path.name.split(".")
        if len(parts) != 4:
            raise Exception(
                f"Invalid release file name. Expected `major.minor.patch.extension` "
                f"(e.g. `1.2.3.json`), but found: {path.name}"
            )
        return cls(major=int(parts[0]), minor=int(parts[1]), patch=int(parts[2]))
