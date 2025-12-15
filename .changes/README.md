# Changelog Tool

This directory contains the changelog tool that is used to generate the changelog
from a set of entry files that can be added to pull requests without risking
merge conflicts.

## Usage

### When Writing a Pull Request

Before submitting a pull request, run the `new-change` script. If called
without arguments, it will prompt for all the information it needs. It will
then store that information in a file in the `next-release` directory that you
must commit.

The tool will ask for an optional pull request number. If you haven't opened a
pull request yet, this is fine. Simply do not fill in that line, and when you
do open a pull request a bot will automatically add a pull request comment with
a change suggestion that adds it.

To get details about optional arguments to the command, run `new-change -h`.

#### Types of changes

The following are the types defined for pull requests:

* **feature** - A larger feature or change in behavior, usually resulting in a
  minor version bump.
* **bugfix** - Fixing a bug in an existing code path.
* **documentation** - A documentation-only change.
* **break** - A breaking change, be it a break-fix or a breaking feature change.
* **other** - Any change that does not fit into another category.

### When Releasing Smithy

Before performing a release, ensure that every pull request since the last
release has an associated changelog entry. If any entries are missing, use
the `new-change` tool as described above to add them in.

You may optionally edit or combine the entries as is necessary. If you combine
entries, ensure that the combined entry contains all of the relevant pr links.

Once the entries have been verified, use the `render` tool to combine the
staged entries and generate the changelog file. From the `.changes` directory,
run the following:

```sh
> uv run release
```

Then commit the changelog, version file, and the `.changes` directory:

```sh
> cd ..
> git add VERSION CHANGELOG.md .changes
```

## Development

The changelog tool is a python project managed by [uv](https://docs.astral.sh/uv/).
While most usage doesn't require worrying about the underlying python code
due to a lack of dependencies, developers must make use of `uv` to lint and
format code before committing.

```sh
> uv sync --all-groups
> uv run ruff check --fix
> uv run ruff format
> uv run pyright
```

uv will manage keeping a virtual python environment up to date and ensure that the
right versions of these tools is used.

uv can also be used to run scripts:

```sh
> uv sync --all-groups
> uv run render
```

## Design

The Smithy changelog tool is designed to make releases easier to automate by
removing the manual effort of assembling the changelog for every release without
introducing regular merge conflicts in pull requests. This follows the same
overall design used by many AWS SDKs, but adapted to the formatting and
requirements of Smithy.

The changelog tool is a set of scripts written in Python. Python is used instead
of Java for its simplicity and ease of writing command line scripts.

These scripts create and manage two sets of JSON files that describe changes and
releases. The `new-change` script creates a JSON file that describes a single
change.

```json
{
  "type": "bugfix",
  "description": "Example description.",
  "pull_requests": [
    "[#9999](https://github.com/smithy-lang/smithy/pull/9999)"
  ]
}
```

These change files are named in the pattern `{type}-{sha1}.json` where `sha1` is
a hash of the JSON itself. This usage of sha1 is intended to produce file names
that are short but not likely to collide.

The change files are stored in `.changes/next-release` until a release happens.
Upon release, all staged changelog entries are deleted and merged into a release
file.

```json
{
  "version": "9.99.99",
  "changes": [
    {
      "type": "bugfix",
      "description": "Example description.",
      "pull_requests": [
        "[#9999](https://github.com/smithy-lang/smithy/pull/9999)"
      ]
    }
  ],
  "date": "2042-12-25"
}
```

These release files are named in the pattern `{version}.json` and are stored in
`.changes/releases`. During a release, all of the release files are read and
rendered into the new `CHANGELOG.md` using the `render` script.

### Special-case Releases

A very small number of releases need to have more complex changelog entries than
the standard formatting allows. In these cases, a hand-written markdown file
represents the release in `.changes/releases` instead of a JSON file. Currently
this is only used in the 1.0.0 release, and likely will only be needed for
similarly major releases.

### GitHub Automation

#### Pull Requests

GitHub automation will exist to reduce the chances of prs going without
changelog entries. A comment will be automatically posted to pull requests if
they lack a staged changelog entry. This comment will remind the requester to
create one if necessary and instruct them how to do it.

If a pull request contains a staged changelog entry, but that entry lacks a link
to the pull request, a review comment will be posted. This review comment will
include a commitable suggestion that adds the link. Adding the link via a review
comment is done because it allows the requester to review the change before it
is added. Additionally, pushing a commit would be difficult as the permissions
and process vary depending on whether the pr originated from a fork.

These actions will be performed by the `amend` script.

#### Releases

A manually-triggered GitHub workflow will bump the Smithy version, gather the
pending changes into a release definition, and render the new changelog. It will
then create a pull request with these changes.

The workflow will have an input argument to select either a major or minor version
bump. The default will depend on the types of staged changes. If there are any
breaking or feature changes, a minor version bump will be the default. Otherwise,
a patch version bump will be the default.

### Alternatives

#### Enforce PRs Update Markdown

An alternative to a changelog tool would be to enforce that pull requests
include manual updates to `CHANGELOG.md`. Since every pull request would be
updating the same lines of that file, this would cause churn in most pull
requests when there is an inevitable merge conflict.

Building automation around this would be more difficult and error-prone. While
markdown is relatively straightforward to parse, it is nevertheless more
difficult than working with JSON. Performing releases, making formatting
changes, detecting new entries (or the lack thereof), all would be more
difficult, requiring more development and maintenance effort that could better
be spent working on features.

#### Do Nothing

Doing nothing means that whoever runs a release must continue to do the manual
effort of putting together release notes prior to running a release. This can be
time-consuming when there have been a lot of pull requests. As with any manual
effort, especially repetitive manual effort, this can be error prone. A PR can
easily be missed when reviewing introduced features, and context can be lost
when writing release notes for a feature you neither wrote nor reviewed.
