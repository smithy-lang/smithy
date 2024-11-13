# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import json
import os
from dataclasses import asdict
from datetime import date

from . import NEXT_RELEASE_DIR, RELEASES_DIR, Change, Release


def release(version: str, release_date: str | None) -> None:
    release_date = release_date or date.today().isoformat()
    entries: list[Change] = []
    for entry in NEXT_RELEASE_DIR.glob("*.json"):
        entries.append(Change.from_json(json.loads(entry.read_text())))
        entry.unlink()

    if not entries:
        raise ValueError(
            "To conduct a release, there must be at least one changelog entry."
        )

    result = Release(version=version, date=release_date, changes=entries)

    if not RELEASES_DIR.is_dir():
        os.makedirs(RELEASES_DIR)

    release_file = RELEASES_DIR / f"{version}.json"
    release_file.write_text(json.dumps(asdict(result), indent=2, default=str) + "\n")
