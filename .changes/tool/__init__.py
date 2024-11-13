# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
from enum import Enum
from typing import TypedDict, Required, NotRequired
from pathlib import Path


CHANGES_DIR = Path(__file__).absolute().parent.parent
NEXT_RELEASE_DIR = CHANGES_DIR / "next-release"
RELEASES_DIR = CHANGES_DIR / "releases"


class ChangeType(Enum):
    FEATURE = "Features"
    BUGFIX = "Bug Fixes"
    DOCUMENTATION = "Documentation"
    BREAK = "Breaking Changes"
    OTHER = "Other"


class Change(TypedDict):
    type: Required[ChangeType]
    description: Required[str]
    pull_requests: NotRequired[list[str]]


class Release(TypedDict):
    version: Required[str]
    date: Required[str]
    changes: Required[list[Change]]
