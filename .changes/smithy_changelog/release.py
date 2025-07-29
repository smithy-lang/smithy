# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import os
from argparse import ArgumentParser
from pathlib import Path

from . import (
    NEXT_RELEASE_DIR,
    RELEASES_DIR,
    REPO_ROOT,
    Bump,
    Change,
    ChangeType,
    Release,
    Version,
)
from .render import render


def main() -> None:
    parser = ArgumentParser(
        description=("Perform a release, updating the changelog and version number.")
    )
    version_group = parser.add_mutually_exclusive_group()
    version_group.add_argument(
        "-v", "--version", type=Version.from_str, help="The version to release."
    )
    version_group.add_argument(
        "-b",
        "--bump",
        type=str,
        choices=["minor", "patch"],
        help=(
            "Whether to bump the minor version or patch version. If not set, the "
            "default value will depend on the staged changes. If there are any "
            "feature or breaking changes, minor will be used as default. Otherwise, "
            "patch is the default. The most recent release in the releases directory "
            "will be used as the base."
        ),
    )
    parser.add_argument(
        "--version-file",
        type=lambda p: Path(p).absolute(),
        help="A file to write the new version to.",
        default=REPO_ROOT / "VERSION",
    )
    parser.add_argument(
        "-d",
        "--release-date",
        type=str,
        help="""\
            The date of the release in ISO format (e.g. 2024-11-13). If not set, \
            today's date, according to your local time zone, will be used.""",
    )
    parser.add_argument(
        "-t",
        "--changelog-title",
        help="The top-level title to use for the changelog file.",
    )
    parser.add_argument(
        "--changelog-file",
        help="The file to write the changelog to.",
        type=lambda p: Path(p).absolute(),
        default=REPO_ROOT / "CHANGELOG.md",
    )
    args = parser.parse_args()
    release(
        version=args.version,
        bump=args.bump,
        version_file=args.version_file,
        release_date=args.release_date,
        changelog_title=args.changelog_title,
        changelog_file=args.changelog_file,
    )


def release(
    version: Version | None,
    bump: Bump | None,
    version_file: Path | None,
    release_date: str | None,
    changelog_title: str | None,
    changelog_file: Path,
) -> None:
    print("Gathering staged changes for release")
    changes: list[Change] = []

    default_bump: Bump = "patch"
    for entry in NEXT_RELEASE_DIR.glob("*.json"):
        print(f"Found staged changelog entry: {entry}")
        change = Change.read(entry)
        if change.type in (ChangeType.FEATURE, ChangeType.BREAK):
            default_bump = "minor"
        changes.append(change)
        entry.unlink()

    if not changes:
        raise ValueError(
            """\
            To conduct a release, there must be at least one staged changelog entry \
            in the next-release folder."""
        )

    if not RELEASES_DIR.is_dir():
        os.makedirs(RELEASES_DIR)

    bump = bump or default_bump
    version = version or _compute_version(bump, version_file)
    result = Release(version=version, date=release_date, changes=changes)

    release_file = RELEASES_DIR / f"{version}.json"
    print(f"Writing combined release to {release_file}")
    result.write(release_file)

    print(f"Rendering new changelog and writing it to: {changelog_file}")
    rendered = render(title=changelog_title)
    changelog_file.write_text(rendered)

    if version_file:
        print(f"Writing version to file: {version_file}")
        version_file.write_text(str(version))


def _compute_version(bump: Bump, version_file: Path | None) -> Version:
    base: Version
    if version_file is not None:
        base = Version.from_str(version_file.read_text().strip())
    else:
        release_files = Release.get_release_files(RELEASES_DIR)
        if not release_files:
            raise Exception(
                "Unable to compute new release version because there are no existing "
                "release files to base the version on."
            )

        base = Version.from_path(release_files[0])

    return base.bump(bump)
