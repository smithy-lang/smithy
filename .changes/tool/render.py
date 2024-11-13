# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
from . import RELEASES_DIR, Release, ChangeType, Change
import json

TITLE = "Smithy Changelog"


def render() -> None:
    rendered = f"# {TITLE}\n\n"
    for release in get_releases():
        rendered += f"## {release['version']} ({release['date']})\n\n"
        entries = collect_entries(release)

        for change_type, changes in entries.items():
            rendered += f"### {change_type.value}\n\n"

            for change in changes:
                rendered += render_change(change)

    print(rendered)


def render_change(change: Change) -> str:
    rendered = f"* {change['description']}"
    if prs := change.get("pull_requests"):
        rendered += f"({', '.join(prs)})"
    return rendered


def collect_entries(release: Release) -> dict[ChangeType, list[Change]]:
    result: dict[ChangeType, list[Change]] = {}
    for change in release["changes"]:
        if change["type"] not in result:
            result[change["type"]] = []
        result[change["type"]].append(change)
    return result


def get_releases() -> list[Release]:
    releases: list[Release] = []
    for release_file in RELEASES_DIR.glob("*.json"):
        releases.append(json.loads(release_file.read_text()))
    return sorted(releases, key=lambda r: r["date"])
