# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import json
import os
import subprocess
from dataclasses import asdict
from pathlib import Path

from . import CHANGES_DIR, Change

REPO_ROOT = CHANGES_DIR.parent


def amend(
    *,
    pr_number: str | None = None,
    repository: str | None = None,
    base: str | None = None,
    commit: bool = False,
    push: bool = False,
) -> None:
    commit = commit or push
    pr_ref = get_pr_ref(pr_number=pr_number, repository=repository)

    amended_files = False
    for change_file, change in get_new_changes(base).items():
        if not change.pull_requests:
            change.pull_requests = [pr_ref]
            change_file.write_text(
                json.dumps(asdict(change), indent=2, default=str) + "\n"
            )

            amended_files = True
            if commit:
                subprocess.run(f"git add {change_file}", check=True, shell=True)

    if commit and amended_files:
        subprocess.run(
            f'git commit -m "Add PR link to changelog for #{pr_number}"',
            check=True,
            shell=True,
        )

        if push:
            subprocess.run(f"git push", check=True, shell=True)


def get_pr_ref(pr_number: str | None, repository: str | None):
    repository = repository or os.environ.get("GITHUB_REPOSITORY", "smithy-lang/smithy")

    if not pr_number:
        pr_number = os.environ.get("GITHUB_REF_NAME")
        if pr_number is None:
            raise ValueError(
                """\
                The pr number to amend onto entries MUST be set. This can be done as \
                an argument to the command, or via the GITHUB_REF_NAME environment \
                varaible as set by GitHub actions."""
            )

        # The GitHub environment variable is in the form "<pr_number>/merge", but
        # we only want the pr number.
        pr_number = pr_number.split("/")[0]

    base_url = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
    return f"[#{pr_number}]({base_url}/{repository}/pull/{pr_number})"


def get_new_changes(base: str | None) -> dict[Path, Change]:
    base = base or os.environ.get("GITHUB_HEAD_REF", "main")
    result = subprocess.run(
        f"git diff {base} --name-only", check=True, shell=True, capture_output=True
    )
    new_changes: dict[Path, Change] = {}
    for changed_file in result.stdout.decode("utf-8").splitlines():
        stripped = changed_file.strip()
        if stripped.startswith(".changes/next-release") and stripped.endswith(".json"):
            file = REPO_ROOT / stripped
            new_changes[file] = Change.from_json(json.loads(file.read_text()))
    return new_changes
