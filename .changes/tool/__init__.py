# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import datetime
import json
from dataclasses import asdict, dataclass, field
from enum import Enum
from pathlib import Path
from typing import Any, Self

CHANGES_DIR = Path(__file__).absolute().parent.parent
NEXT_RELEASE_DIR = CHANGES_DIR / "next-release"
RELEASES_DIR = CHANGES_DIR / "releases"
REPO_ROOT = CHANGES_DIR.parent


class ChangeType(Enum):
    FEATURE = "Features", 1
    BUGFIX = "Bug Fixes", 2
    DOCUMENTATION = "Documentation", 3
    BREAK = "Breaking Changes", 0
    OTHER = "Other", 4

    def __init__(self, section_title: str, order: int) -> None:
        self.section_title = section_title
        self.order = order

    def __str__(self) -> str:
        return self.name.lower()

    def __lt__(self, other: Self) -> bool:
        return self.order < other.order


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

    @classmethod
    def read(cls, file: Path) -> Self:
        return cls.from_json(json.loads(file.read_text()))

    def write(self, file: Path | None = None) -> str:
        contents = json.dumps(asdict(self), indent=2, default=str) + "\n"
        if file is not None:
            file.write_text(contents)
        return contents


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
        return dict(sorted(result.items()))

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> Self:
        return cls(
            version=data["version"],
            changes=[Change.from_json(c) for c in data["changes"]],
            date=data["date"],
        )

    @classmethod
    def read(cls, file: Path) -> Self:
        return cls.from_json(json.loads(file.read_text()))

    def write(self, file: Path | None = None) -> str:
        contents = json.dumps(asdict(self), indent=2, default=str) + "\n"
        if file is not None:
            file.write_text(contents)
        return contents

    def __lt__(self, other: Self) -> bool:
        return self.date < other.date
