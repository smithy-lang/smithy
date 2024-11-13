# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import datetime
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path
from typing import Any, Self

CHANGES_DIR = Path(__file__).absolute().parent.parent
NEXT_RELEASE_DIR = CHANGES_DIR / "next-release"
RELEASES_DIR = CHANGES_DIR / "releases"


class ChangeType(Enum):
    FEATURE = "Features"
    BUGFIX = "Bug Fixes"
    DOCUMENTATION = "Documentation"
    BREAK = "Breaking Changes"
    OTHER = "Other"

    def __str__(self) -> str:
        return self.name.lower()


@dataclass
class Change:
    type: ChangeType
    description: str
    pull_requests: list[str] = field(default_factory=list)

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> Self:
        return cls(
            type=ChangeType[data["type"].upper()],
            description=data["description"],
            pull_requests=data.get("pull_requests") or [],
        )


def _today() -> str:
    return datetime.date.today().isoformat()


@dataclass
class Release:
    version: str
    changes: list[Change]
    date: str = field(default_factory=_today)

    def change_map(self) -> dict[ChangeType, list[Change]]:
        result: dict[ChangeType, list[Change]] = {}
        for change in self.changes:
            if change.type not in result:
                result[change.type] = []
            result[change.type].append(change)
        return result

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> Self:
        return cls(
            version=data["version"],
            changes=[Change.from_json(c) for c in data["changes"]],
            date=data["date"],
        )

    def __lt__(self, other: Self) -> bool:
        return self.date < other.date
