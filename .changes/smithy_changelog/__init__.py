# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import datetime
import json
from dataclasses import asdict, dataclass, field
from enum import Enum
from pathlib import Path
from typing import Any, Literal, Self

CHANGES_DIR = Path(__file__).absolute().parent.parent
NEXT_RELEASE_DIR = CHANGES_DIR / "next-release"
RELEASES_DIR = CHANGES_DIR / "releases"
REPO_ROOT = CHANGES_DIR.parent


class ChangeType(Enum):
    """The type of a change.

    This enum also embeds the name of the sections for each change type as well
    as the order which those sections should be written in.
    """

    FEATURE = "Features", 1
    BUGFIX = "Bug Fixes", 2
    DOCUMENTATION = "Documentation", 3
    BREAK = "Breaking Changes", 0
    OTHER = "Other", 4

    def __init__(self, section_title: str, order: int) -> None:
        """Constructs a ChangeType.

        This should not be called manually, it should only be called by the Enum
        metaclass.

        :param section_title: The title of the change type's changelog section.
        :param order: The order that this change type's section appears in the
            changelog.
        """
        self.section_title = section_title
        self.order = order

    def __str__(self) -> str:
        return self.name.lower()

    def __lt__(self, other: Self) -> bool:
        return self.order < other.order


@dataclass
class Change:
    """A representation of an individual change."""

    type: ChangeType
    """The type of change."""

    description: str
    """A description of the change."""

    pull_requests: list[str] = field(default_factory=list[str])
    """A list of pull requests associated with the change as markdown links.

    For example, `[#9999](https://github.com/smithy-lang/smithy/pulls/9999)`
    """

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> Self:
        """Loads a JSON representation of a change.

        :param data: A JSON representation of a change.
        """
        return cls(
            type=ChangeType[data["type"].upper()],
            description=data["description"],
            pull_requests=data.get("pull_requests") or [],
        )

    @classmethod
    def read(cls, file: Path) -> Self:
        """Loads a JSON representation of a change from a file.

        :param file: A path to a JSON file containing a JSON representation
            of a change.
        """
        return cls.from_json(json.loads(file.read_text()))

    def write(self, file: Path | None = None) -> str:
        """Writes change to a JSON string, and optionally to a file.

        :param file: A file to write to.

        :returns: The JSON representation of the change.
        """
        contents = json.dumps(asdict(self), indent=2, default=str) + "\n"
        if file is not None:
            file.write_text(contents)
        return contents


type Bump = Literal["minor", "patch"]
"""The types of version bumps that may be used."""


@dataclass
class Version:
    """A representation of a three-part numeric version.

    This is primarily used for sorting, but also used for handling incrementation
    logic.
    """

    major: int
    minor: int
    patch: int

    def bump(self, bump: Bump) -> "Version":
        """Bump the version accoring to the specified bump type.

        :returns: An incremented :py:class:`Version`
        """
        match bump:
            case "minor":
                return Version(major=self.major, minor=self.minor + 1, patch=0)
            case "patch":
                return Version(major=self.major, minor=self.minor, patch=self.patch + 1)

    @classmethod
    def from_str(cls, version: str) -> Self:
        """Creates a Version from a string representation."""
        parts = version.split(".", 2)
        if len(parts) != 3:
            raise Exception(
                f"Invalid version. Expected `major.minor.patch` "
                f"(e.g. `1.2.3`), but found: {version}"
            )
        return cls(major=int(parts[0]), minor=int(parts[1]), patch=int(parts[2]))

    @classmethod
    def from_path(cls, path: Path) -> Self:
        """Creates a version from a file representation.

        The file name is expected to be in the form `major.minor.patch.extention`.
        """
        parts = path.name.split(".", 3)
        if len(parts) != 4:
            raise Exception(
                f"Invalid version. Expected `major.minor.patch.extension` "
                f"(e.g. `1.2.3.json`), but found: {path.name}"
            )
        return cls(major=int(parts[0]), minor=int(parts[1]), patch=int(parts[2]))

    def __lt__(self, other: Self) -> bool:
        if self.major != other.major:
            return self.major < other.major
        if self.minor != other.minor:
            return self.minor < other.minor
        return self.patch < other.patch

    def __str__(self) -> str:
        return f"{self.major}.{self.minor}.{self.patch}"


@dataclass(init=False)
class Release:
    """A representation of all the changes in a release."""

    version: Version
    """The release version."""

    changes: list[Change]
    """The list of changes in the release."""

    date: str
    """The date of the release in ISO format (e.g. 2025-07-29)"""

    def __init__(
        self, version: str | Version, changes: list[Change], date: str | None
    ) -> None:
        """Initializes a Release.

        :param version: The version of the release, optionally as a string.
        :param changes: The list of changes included in the release.
        :param date: The date of the release in ISO format (e.g. 2025-07-29) or
            today's date in ISO format if not set.
        """
        self.version = (
            version if isinstance(version, Version) else Version.from_str(version)
        )
        self.changes = changes
        self.date = date or datetime.date.today().isoformat()

    def change_map(self) -> dict[ChangeType, list[Change]]:
        """Returns a map of change type to change for changes in the release."""
        result: dict[ChangeType, list[Change]] = {}
        for change in self.changes:
            if change.type not in result:
                result[change.type] = []
            result[change.type].append(change)
        return dict(sorted(result.items()))

    @classmethod
    def from_json(cls, data: dict[str, Any]) -> Self:
        """Loads a release from a JSON representation.

        :param data: The JSON representation of the release.
        """
        return cls(
            version=data["version"],
            changes=[Change.from_json(c) for c in data["changes"]],
            date=data["date"],
        )

    @classmethod
    def read(cls, file: Path) -> Self:
        """Reads a release from a JSON file.

        :param file: The file containing the release JSON.
        """
        return cls.from_json(json.loads(file.read_text()))

    def write(self, file: Path | None = None) -> str:
        """Writes a release to a JSON string, and optionally to a file.

        :param file: A file to write the release JSON to.

        :returns: The JSON representation of the release as a string.
        """
        data = asdict(self)
        data["version"] = str(self.version)
        contents = json.dumps(data, indent=2, default=str) + "\n"
        if file is not None:
            file.write_text(contents)
        return contents

    @staticmethod
    def get_release_files(release_dir: Path = RELEASES_DIR) -> list[Path]:
        """Gets a sorted list of files in the release directory.

        :param release_dir: The directory to search for releases.
        """
        release_files = list(release_dir.glob("*.json"))
        release_files.extend(release_dir.glob("*.md"))
        return sorted(release_files, key=lambda p: Version.from_path(p), reverse=True)

    def __lt__(self, other: Self) -> bool:
        return self.version < other.version
