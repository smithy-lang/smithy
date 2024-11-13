# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import json
from datetime import date
import os

from . import NEXT_RELEASE_DIR, Change, Release, RELEASES_DIR


def release(version: str, release_date: str | None) -> None:
    release_date = release_date or date.today().isoformat()
    entries: list[Change] = []
    for entry in NEXT_RELEASE_DIR.glob("*.json"):
        entries.append(Change(json.loads(entry.read_text())))
        entry.unlink()

    if not entries:
        raise ValueError(
            "To conduct a release, there must be at least one changelog entry."
        )

    result: Release = {"version": version, "date": release_date, "changes": entries}

    if not RELEASES_DIR.is_dir():
        os.makedirs(RELEASES_DIR)

    release_file = RELEASES_DIR / f"{version}.json"
    release_file.write_text(json.dumps(result, indent=2, default=str) + "\n")
