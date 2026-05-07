---
name: smithy-pr-workflow
description: >
  Guides developers through the Smithy team's GitHub PR workflow for the
  public smithy-lang/smithy repository. Covers commit conventions, changelog
  formatting, build validation, documentation builds, code formatting, and
  review feedback handling. Use when the user wants to create a PR for the
  Smithy repo, address review feedback, fix CI failures, update changelogs,
  run pre-commit validation, or understand the Smithy team's PR conventions.
---

# Smithy Public PR Workflow

This skill covers the conventions and workflow for contributing to the
public `smithy-lang/smithy` GitHub repository. These rules were established
through direct feedback from the Smithy team (Kevin Stich, Manuel Sugawara)
and apply to all PRs targeting the `main` branch.

---

## Golden Rules

1. **Never force push** — create new commits instead. The Smithy team
   squashes at merge. GitHub doesn't have revisions like CRUX, so force
   pushing destroys review context.
2. **Changelog uses past tense** — "Added..." not "Add...", "Fixed..." not
   "Fix...". Commit messages use imperative (present tense), but changelogs
   describe things that happened.
3. **Run `./gradlew clean build`** before every push — this runs all tests
   AND applies code formatting (spotless). If you skip this, CI will fail.
4. **Run docs build** from `/docs` folder for RST validation — `make html`
   (requires Python 3.12+).
5. **No internal references** in public PRs — never mention CloudFormation,
   SmithyImporter, BackplaneControlService, SIM links, code.amazon.com, or
   internal design rationale.

---

## Pre-Push Checklist

Run these before every `git push`:

```bash
# 1. Full build (includes spotless code formatting + all tests)
./gradlew clean build

# 2. Docs build (if you changed .rst files)
cd docs && make html

# 3. Verify no unintended changes from spotless
git diff
```

If spotless reformatted files, stage them in your commit. CI runs the same
checks and will fail if formatting doesn't match.

---

## Changelog Conventions

Changelogs live in `.changes/next-release/` as JSON files. Create one with:

```bash
./.changes/new-change --pull-requests "#NNNN" --type feature --description "Added \`namespace#traitName\` trait and OpenAPI mapper"
```

Rules:
- **Past tense**: "Added...", "Fixed...", "Updated..."
- **Backticks** around trait names: `` `aws.apigateway#traitName` ``
- **Type**: `feature`, `bugfix`, `documentation`, or `other`
- **PR link**: `[#NNNN](https://github.com/smithy-lang/smithy/pull/NNNN)`

Bad: `"Add aws.apigateway#myTrait trait"`
Good: `"Added \`aws.apigateway#myTrait\` trait and OpenAPI mapper"`

---

## Commit Message Style

The Smithy repo uses simple imperative subject lines — no conventional
commits prefix (`feat:`, `fix:`), no scope. Just plain imperative:

```
Add minimumCompressionSize trait and OpenAPI mapper

Add GatewayResponse value class with builder following the same
pattern as AuthorizersTrait and DefineConditionKeysTrait.
```

For review feedback commits:

```
Fix scopes logic, formatting, and docs links

Compute scopes list before the early-return check so the
trait is not required to make auth adjustments. Fix code
formatting. Shorten RST header underline.
```

---

## RST Documentation Rules

These are the most common review feedback items:

### Header Underline Length

The overline and underline must match the title length exactly:

```rst
---------------------------------------
``aws.apigateway#apiKeyRequired`` trait
---------------------------------------
```

NOT:

```rst
------------------------------------------
``aws.apigateway#apiKeyRequired`` trait
------------------------------------------
```

### Capitalization in Prose

When a code block is followed by prose, use lowercase:

```rst
    service Example {
      version: "2019-06-17"
    }

is converted to the following OpenAPI model:
```

NOT `Is converted...` (capital I).

### No "Internal-Only" Notes

Do not add notes like "This trait should be considered internal-only and
not exposed to your customers." The Smithy team found this confusing
("Whose customers?"). All traits in the public repo are public.

### Link External Docs

When mentioning AWS services (Cognito, VPC, etc.), link to their docs:

```rst
A list of `Amazon Cognito user pool`_ ARNs for the authorizer.

.. _Amazon Cognito user pool: https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools.html
```

### Expand Acronyms on First Use

Write "Virtual Private Cloud (VPC)" on first mention, then "VPC" after.

---

## Handling Review Feedback

When the Smithy team leaves review comments:

1. **Create a new commit** for each round of feedback (don't amend)
2. **Apply suggested changes** exactly as suggested when possible
3. **Run `./gradlew clean build`** after making changes
4. **Push to the same branch** — the PR updates automatically
5. **Don't resolve conversations** — let the reviewer resolve them

If Kevin or Manuel apply suggestions directly via GitHub's "Apply
suggestions" feature, pull their commits before pushing yours:

```bash
git fetch fork <branch-name>
git pull fork <branch-name>
```

Watch for merge artifacts from GitHub's suggestion commits — they sometimes
create duplicated lines (especially in RST headers).

---

## Common CI Failures

| Failure | Fix |
|---|---|
| Spotless check failed | Run `./gradlew clean build` (auto-applies formatting) |
| Broken cross-reference | Check RST header underlines match title length |
| Duplicated header | GitHub suggestion commits can create duplicates — check the file |
| Test failure | Run tests locally, check if spotless changed test files |

---

## PR Description Template

Use this structure for PR descriptions:

```markdown
Brief one-paragraph summary of what the PR does.

#### Background

* What do these changes do?
  * Bullet points describing each change
* Why are they important?
  * Motivation and context

#### Testing

* How did you test these changes?
  * Unit tests, integration tests, manual verification

#### Links

* [Relevant AWS docs](URL)
* Issue #, if applicable
```

---

## Builder Pattern for Traits

The Smithy team prefers traits to use the builder pattern (not raw
`ObjectNode` wrapping). Follow these patterns:

- **Map traits** (like `gatewayResponses`, `authorizers`): Use
  `AbstractTraitBuilder`, `BuilderRef.forOrderedMap()`, `ToSmithyBuilder`
- **Value classes** (like `GatewayResponse`, `AuthorizerDefinition`): Use
  `SmithyBuilder`, `ToNode`, `ToSmithyBuilder`, `fromNode()` static method
- **Provider**: Use `setNodeCache(value)` after building

Reference implementations:
- `AuthorizersTrait` + `AuthorizerDefinition` (map trait with value class)
- `DefineConditionKeysTrait` + `ConditionKeyDefinition` (same pattern in IAM)

---

## Notes

- The Smithy team squashes all commits at merge — one commit per PR in
  final history
- Kevin and Manuel review most PRs; they may apply suggestions directly
- CI checks include: build, spotless, spotbugs, javadoc, tests
- The docs build requires Python 3.12+ (for Sphinx 9.1.0)
- PR titles don't need conventional commit prefixes
- Force pushing is only acceptable before the first review (and even then,
  prefer not to)
