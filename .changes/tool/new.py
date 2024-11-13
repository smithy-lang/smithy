# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import tempfile
import string
import os
import subprocess
import re
import hashlib
import json

from . import ChangeType, Change, NEXT_RELEASE_DIR


VALID_CHARS = set(string.ascii_letters + string.digits)
TEMPLATE = """\
# Type should be one of: "feature", "bugfix", "documentation", "break", or "other".
#
# feature:       A larger feature or change in behavior, usually resulting in a
#                minor version bump.
# bugfix:        Fixing a bug in an existing code path.
# documentation: A documentation-only change.
# break:         A breaking change, be it a break-fix or a breaking feature change.
# other:         Any change that does not fit into another category.
type: {change_type}

# (Optional)
# A link to the GitHub pull request that implments the feature. You can use GitHub
# sytle references, which will automatically be replaced with the correct link.
pull request: {pull_requests}

# A brief description of the change. You can use GitHub style references to issues such
# as "fixes #489", "boto/boto3#100", etc. These will get automatically replaced with
# the correct link.
description: {description}
"""


def new_change(
    change_type: ChangeType | None,
    description: str | None,
    pull_requests: list[str] | None,
):
    if change_type is not None and description is not None:
        change: Change = {"type": change_type, "description": description}
        if pull_requests:
            change["pull_requests"] = pull_requests
    else:
        parsed = get_values_from_editor(change_type, description, pull_requests)
        if not parsed:
            return
        change = parsed

    write_new_change(change)


def get_values_from_editor(
    change_type: ChangeType | None,
    description: str | None,
    pull_requests: list[str] | None,
) -> Change | None:
    with tempfile.NamedTemporaryFile("w") as f:
        prs = pull_requests or []
        contents = TEMPLATE.format(
            change_type=change_type.name.lower() if change_type is not None else "",
            description=description or "",
            pull_requests=", ".join(prs),
        )
        f.write(contents)
        f.flush()

        env = os.environ
        editor = env.get("VISUAL", env.get("EDITOR", "vim"))
        p = subprocess.Popen(f"{editor} {f.name}", shell=True)
        p.communicate()

        with open(f.name) as f:
            return parse_filled_in_contents(f.read())


def replace_issue_references(change: Change, repo_name: str):
    description = change["description"]

    def linkify(match: re.Match[str]):
        number = match.group()[1:]
        return f"[{match.group()}](https://github.com/{repo_name}/issues/{number})"

    new_description = re.sub(r"#\d+", linkify, description)
    change["description"] = new_description

    if "pull_requests" in change:
        change["pull_requests"] = [
            re.sub(r"#\d+", linkify, pr) for pr in change["pull_requests"]
        ]


def write_new_change(change: Change):
    if not NEXT_RELEASE_DIR.is_dir():
        os.makedirs(NEXT_RELEASE_DIR)

    contents = json.dumps(change, indent=2, default=str) + "\n"
    contents_digest = hashlib.sha1(contents.encode("utf-8")).hexdigest()
    filename = f"{change['type'].name.lower()}-{contents_digest}.json"

    with open(NEXT_RELEASE_DIR / filename, "w") as f:
        f.write(contents)


def parse_filled_in_contents(contents: str) -> Change | None:
    """Parse filled in file contents and returns parsed dict."""
    if not contents.strip():
        return

    parsed = {}
    lines = iter(contents.splitlines())
    for line in lines:
        line = line.strip()
        if line.startswith("#"):
            continue

        if "type" not in parsed and line.startswith("type:"):
            parsed["type"] = ChangeType[line.split(":")[1].strip().upper()]
        if "pull_requests" not in parsed and line.startswith("pull requests:"):
            parsed["pull_requests"] = [
                pr.strip() for pr in line.split(":")[1].strip().split(",")
            ]
        elif "description" not in parsed and line.startswith("description:"):
            # Assume that everything until the end of the file is part
            # of the description, so we can break once we pull in the
            # remaining lines.
            first_line = line.split(":")[1].strip()
            full_description = "\n".join([first_line] + list(lines))
            parsed["description"] = full_description.strip()
            break

    return Change(parsed)  # type: ignore
