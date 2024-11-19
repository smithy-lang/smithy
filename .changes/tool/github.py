# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

# This module provides functions to interact with the GitHub API. No sort of
# official client is used so that dependencies can be avoided. The standard
# library's http modules are used instead of Requests for the same reason.
import json
import os
import re
import time
from dataclasses import dataclass
from http.client import HTTPResponse
from pathlib import Path
from typing import Generator, Literal, NotRequired, Required, Self, TypedDict
from urllib import request
from urllib.error import HTTPError

from . import REPO_ROOT

GITHUB_API_URL = os.environ.get("GITHUB_API_URL", "https://api.github.com")
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN")
NEXT_PAGE = re.compile(r'(?<=<)([\S]*)(?=>; rel="next")', flags=re.IGNORECASE)

type JSON = str | int | bool | None | list[JSON] | dict[str, JSON]


def post_review_comment(
    repository: str,
    pr_number: str,
    comment: str,
    file: Path,
    start_line: int | None = None,
    end_line: int | None = None,
    allow_duplicate: bool = False,
) -> None:
    commit_sha = os.environ.get("TARGET_SHA")
    if not commit_sha:
        raise ValueError(
            "The TARGET_SHA environment variable must be set to post review comments."
        )

    path = str(file.relative_to(REPO_ROOT))

    if not allow_duplicate:
        for existing_comment in get_review_comments(repository, pr_number):
            if existing_comment["path"] == path and existing_comment["body"] == comment:
                print("Review comment already posted, skipping duplicate.")
                return

    request_body: CreateReviewCommentParams = {
        "body": comment,
        "commit_id": commit_sha,
        "path": path,
    }

    if start_line is not None:
        request_body["side"] = "RIGHT"
        if end_line is not None:
            request_body["start_side"] = "RIGHT"
            request_body["start_line"] = start_line
            request_body["line"] = end_line
        else:
            request_body["line"] = start_line
    elif end_line is not None:
        raise ValueError("If end_line is set, start_line must also be set.")

    resolved_url = f"{GITHUB_API_URL}/repos/{repository}/pulls/{pr_number}/comments"
    _request(
        resolved_url,
        # mypy is too dumb to realize a typeddict is a dict
        request_body,  # type: ignore
    )


type ReviewSide = Literal["LEFT"] | Literal["RIGHT"]


# https://docs.github.com/en/rest/pulls/comments?apiVersion=2022-11-28#create-a-review-comment-for-a-pull-request
class CreateReviewCommentParams(TypedDict):
    body: Required[str]
    commit_id: Required[str]
    path: Required[str]
    start_line: NotRequired[int]
    line: NotRequired[int]
    start_side: NotRequired[ReviewSide]
    side: NotRequired[ReviewSide]
    subject_type: NotRequired[Literal["line"] | Literal["file"]]


class User(TypedDict):
    login: Required[str]


class Comment(TypedDict):
    user: Required[User]
    body: str


class ReviewComment(Comment):
    path: Required[str]


# https://docs.github.com/en/rest/pulls/comments?apiVersion=2022-11-28#list-review-comments-on-a-pull-request
def get_review_comments(
    repository: str,
    pr_number: str,
) -> Generator[ReviewComment]:
    url = f"{GITHUB_API_URL}/repos/{repository}/pulls/{pr_number}/comments?per_page=100"
    for response in _paginate(url):
        if not isinstance(response.body, list):
            raise ValueError(
                f"Expected list body, but found {type(response.body)}: {response.body}"
            )
        for comment in response.body:
            yield comment  # type: ignore


# https://docs.github.com/en/rest/issues/comments?apiVersion=2022-11-28#create-an-issue-comment
def post_comment(
    repository: str,
    pr_number: str,
    comment: str,
    allow_duplicate: bool = False,
) -> None:
    if not allow_duplicate:
        for existing_comment in get_comments(repository, pr_number):
            if existing_comment["body"] == comment:
                print("Comment already posted, skipping duplicate.")
                return

    _request(
        url=f"{GITHUB_API_URL}/repos/{repository}/issues/{pr_number}/comments",
        body={"body": comment},
    )


# https://docs.github.com/en/rest/issues/comments?apiVersion=2022-11-28#list-issue-comments
def get_comments(
    repository: str,
    pr_number: str,
) -> Generator[Comment]:
    url = (
        f"{GITHUB_API_URL}/repos/{repository}/issues/{pr_number}/comments?per_page=100"
    )
    for response in _paginate(url):
        if not isinstance(response.body, list):
            raise ValueError(
                f"Expected list body, but found {type(response.body)}: {response.body}"
            )
        for comment in response.body:
            yield comment  # type: ignore


@dataclass
class JSONResponse:
    headers: dict[str, str]
    body: JSON
    status: int

    @classmethod
    def from_http_response(cls, http_response: HTTPResponse) -> Self:
        return cls(
            headers={k.lower(): v for k, v in http_response.getheaders()},
            body=json.loads(http_response.read()),
            status=http_response.status,
        )


def _request(url: str, body: JSON = None) -> JSONResponse:
    if not GITHUB_TOKEN:
        raise ValueError(
            "The GITHUB_TOKEN environment variable must be set to post review comments."
        )

    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {GITHUB_TOKEN}",
        "X-GitHub-Api-Version": "2022-11-28",
    }

    if body is not None:
        req = request.Request(
            url=url, headers=headers, data=json.dumps(body, indent=2).encode("utf-8")
        )
    else:
        req = request.Request(url=url, headers=headers)

    print(f"Sending GitHub API request to {url} with body: {body}")

    try:
        http_response = request.urlopen(req)
    except HTTPError as e:
        raise GitHubAPIError(e) from e

    response = JSONResponse.from_http_response(http_response)
    print(f"Received response: {response}")
    return response


# Use a generator so that callers can break out before pagination is complete.
def _paginate(url: str) -> Generator[JSONResponse]:
    print(f"Paginating GitHub api: {url}")
    while True:
        response = _request(url)
        yield response

        if match := NEXT_PAGE.search(response.headers.get("link", "")):
            url = match.group()
            print("Waiting one minute before requesting the next page.")
            time.sleep(60)
        else:
            print("Done paginating.")
            break


class GitHubAPIError(Exception):
    def __init__(self, response: HTTPError) -> None:
        formatted_body = json.dumps(
            json.loads(response.read().decode("utf-8")), indent=2
        )
        super().__init__(f"GitHub API request failed:\n\n{formatted_body}")
