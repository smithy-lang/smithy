# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import json

from . import RELEASES_DIR, Change, Release

TITLE = "Smithy Changelog"


def render() -> None:
    rendered = f"# {TITLE}\n\n"
    for release in get_releases():
        rendered += f"## {release.version} ({release.date})\n\n"

        for change_type, changes in release.change_map().items():
            rendered += f"### {change_type.value}\n\n"

            for change in changes:
                rendered += render_change(change)

    print(rendered)


def render_change(change: Change) -> str:
    rendered = f"* {change.description}"
    if prs := change.pull_requests:
        rendered += f"({', '.join(prs)})"
    return rendered


def get_releases() -> list[Release]:
    releases: list[Release] = []
    for release_file in RELEASES_DIR.glob("*.json"):
        releases.append(Release.from_json(json.loads(release_file.read_text())))
    return sorted(releases)
