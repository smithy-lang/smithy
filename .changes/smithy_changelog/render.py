# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
from argparse import ArgumentParser

from . import RELEASES_DIR, Change, Release
from .release import release as _release

TITLE = "Changelog"


def main() -> None:
    parser = ArgumentParser(
        description="""\
            Render the changelog as markdown, optionally including pending features \
            as a new release.""",
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

    render()


def render() -> None:
    rendered = f"# {TITLE}\n\n"
    for release in get_releases():
        rendered += f"## {release.version} ({release.date})\n\n"

        for change_type, changes in release.change_map().items():
            rendered += f"### {change_type.section_title}\n\n"

            for change in changes:
                rendered += render_change(change)

            rendered += "\n"

    print(rendered)


def render_change(change: Change) -> str:
    rendered = f"* {change.description}"
    if prs := change.pull_requests:
        rendered += f"({', '.join(prs)})"
    return rendered + "\n"


def get_releases() -> list[Release]:
    releases: list[Release] = []
    for release_file in RELEASES_DIR.glob("*.json"):
        releases.append(Release.read(release_file))
    return sorted(releases)
