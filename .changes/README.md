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

### When Releasing Smithy

Before performing a release, ensure that every pull request since the last
release has an associated changelog entry. If any entries are missing, use
the `new-change` tool as described above to add them in.

You may optionally edit or combine the entries as is necessary. If you combine
entries, ensure that the combined entry contains all of the relevant pr links.

Once the entries have been verified, use the `render` tool to combine the
staged entries and generate the changelog file. From the root of the Smithy
repository, run the following with the version being released:

```sh
> ./.changes/render --release-version RELEASE_VERSION > CHANGELOG.md
```

If the `VERSION` file has already been updated, this can be simplified:

```sh
> ./.changes/render --release-version "$(cat VERSION)" > CHANGELOG.md
```

Then commit the changelog and the `.changes` directory:

```sh
> git add CHANGELOG.md .changes
```
