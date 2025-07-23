# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import os
from datetime import date

from . import NEXT_RELEASE_DIR, RELEASES_DIR, Change, Release


def release(version: str, release_date: str | None) -> None:
    version = version.strip()
    print("Gathering staged changes for release")
    release_date = release_date or date.today().isoformat()
    changes: list[Change] = []

    for entry in NEXT_RELEASE_DIR.glob("*.json"):
        print(f"Found staged changelog entry: {entry}")
        changes.append(Change.read(entry))
        entry.unlink()

    if not changes:
        raise ValueError(
            """\
            To conduct a release, there must be at least one staged changelog entry \
            in the next-release folder."""
        )

    result = Release(version=version, date=release_date.strip(), changes=changes)

    if not RELEASES_DIR.is_dir():
        os.makedirs(RELEASES_DIR)

    release_file = RELEASES_DIR / f"{version}.json"
    print(f"Writing combined release to {release_file}")
    result.write(release_file)
