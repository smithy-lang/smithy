# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import os
import subprocess
from argparse import ArgumentParser
from pathlib import Path

from . import REPO_ROOT, Change
from .github import post_comment, post_review_comment

DEFAULT_REPO = "smithy-lang/smithy"
GITHUB_URL = os.environ.get("GITHUB_SERVER_URL", "https://github.com")


def main() -> None:
    parser = ArgumentParser(
        description="""\
            Amend recently-introduced changelog entries to include a pull request \
            link. This is intended to be run in GitHub as an action, but can be run \
            manually by specifying parameters GitHub would otherwise provide in \
            environment variables.

            This only checks entries that have been staged in the current branch, \
            using git to get a list of newly introduced files. If the entry already \
            has one or more associated pull requests, it is not amended.""",
    )
    parser.add_argument(
        "-n",
        "--pull-request-number",
        required=True,
        help="The numeric identifier for the pull request.",
    )
    parser.add_argument(
        "-r",
        "--repository",
        help="""\
            The name of the repository, defaulting to 'smithy-lang/smithy'. This is \
            provided by GitHub in the GITHUB_REPOSITORY environment variable.""",
    )
    parser.add_argument(
        "-b",
        "--base",
        help="""\
            The name of the base branch to diff against, defaulting to 'main'. This \
            is provided by GitHub in the GITHUB_BASE_REF environment variable.""",
    )
    parser.add_argument(
        "-c",
        "--review-comment",
        action="store_true",
        default=False,
        help="""\
            Instead of amending the local files on disk, post a review comment on the \
            PR. This will also post a normal comment if no changelog entry is found.""",
    )

    args = parser.parse_args()
    amend(
        base=args.base,
        repository=args.repository,
        pr_number=args.pull_request_number,
        review_comment=args.review_comment,
    )


def amend(
    *,
    pr_number: str,
    repository: str | None = None,
    base: str | None = None,
    review_comment: bool = False,
) -> None:
    repository = repository or os.environ.get("GITHUB_REPOSITORY", DEFAULT_REPO)
    pr_ref = f"[#{pr_number}]({GITHUB_URL}/{repository}/pull/{pr_number})"

    changes = get_new_changes(base)
    if not changes and review_comment:
        print("No changelog found, adding reminder comment.")
        description = os.environ.get("PR_TITLE", "Example description").replace(
            '"', '\\"'
        )
        comment = (
            "This pull request does not contain a staged changelog entry. To create "
            "one, use the `./.changes/new-change` command. For example:\n\n"
            f'```\n./.changes/new-change --pull-requests "#{pr_number}" '
            f'--type feature --description "{description}"\n```\n\n'
            "Make sure that the description is appropriate for a changelog entry and "
            "that the proper feature type is used. See [`./.changes/README`]("
            f"{GITHUB_URL}/{repository}/tree/main/.changes/README) or run "
            "`./.changes/new-change -h` for more information."
        )
        post_comment(
            repository=repository,
            pr_number=pr_number,
            comment=comment,
        )

    for change_file, change in changes.items():
        if not change.pull_requests:
            print(f"Amending changelog entry without associated prs: {change_file}")
            change.pull_requests = [pr_ref]

            if review_comment:
                print("Posting amended change as a review comment.")
                comment = (
                    "Staged changelog entries should have an associated pull request "
                    "set. Commit this suggestion to associate this changelog entry "
                    "with this PR.\n\n"
                    f"```suggestion\n{change.write().strip()}\n```"
                )
                post_review_comment(
                    repository=repository,
                    pr_number=pr_number,
                    comment=comment,
                    file=change_file,
                    start_line=1,
                    end_line=len(change_file.read_text().splitlines()),
                )
            else:
                print("Writing amended change to disk.")
                change.write(change_file)


def get_new_changes(base: str | None) -> dict[Path, Change]:
    base = base or os.environ.get("GITHUB_BASE_REF", "main")
    print(f"Running a diff against base branch: {base}")
    result = subprocess.run(
        f"git diff origin/{base} --name-only",
        check=True,
        shell=True,
        capture_output=True,
    )

    new_changes: dict[Path, Change] = {}
    for changed_file in result.stdout.decode("utf-8").splitlines():
        stripped = changed_file.strip()
        if stripped.startswith(".changes/next-release") and stripped.endswith(".json"):
            file = REPO_ROOT / stripped
            print(f"Discovered newly staged changelog entry: {file}")
            new_changes[file] = Change.read(file)
    return new_changes
