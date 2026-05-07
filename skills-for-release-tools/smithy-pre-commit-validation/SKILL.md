---
name: smithy-pre-commit-validation
description: >
  Automated pre-commit validation checklist for the smithy-lang/smithy
  repository. Ensures code formatting, changelog conventions, RST docs,
  and build integrity before pushing commits. Use when the user wants to
  validate changes before pushing, fix build failures, ensure changelog
  format is correct, verify RST documentation, or run the full Smithy
  validation pipeline.
---

# Smithy Pre-Commit Validation

This skill provides an automated validation checklist that must pass before
pushing any commit to the `smithy-lang/smithy` repository. It catches the
most common PR feedback items before they reach reviewers.

---

## Full Validation Pipeline

Run these steps in order. Stop at the first failure and fix it before
continuing.

### Step 1: Code Formatting + Build + Tests

```bash
./gradlew clean build
```

This single command does everything:
- Compiles all modules
- Runs spotless (auto-applies code formatting)
- Runs spotbugs (static analysis)
- Runs all unit and integration tests
- Generates javadoc

If spotless reformatted any files, they'll show up in `git diff`. Stage
them in your commit.

### Step 2: Verify Spotless Didn't Change Anything Unexpected

```bash
git diff --name-only
```

If files were changed that you didn't touch, spotless reformatted them.
This is expected for files you modified — stage them. If unrelated files
changed, something is wrong.

### Step 3: Changelog Validation

Check all changelog files in `.changes/next-release/`:

```bash
cat .changes/next-release/*.json
```

Verify each entry:
- [ ] Uses **past tense** ("Added...", "Fixed...", "Updated...")
- [ ] Trait names wrapped in **backticks** (`` `namespace#traitName` ``)
- [ ] Correct **PR number** in pull_requests array
- [ ] Correct **type** (feature, bugfix, documentation, other)

### Step 4: RST Documentation Validation

If you modified any `.rst` files:

```bash
cd docs && make html
```

This requires Python 3.12+. If unavailable, manually verify:
- [ ] Header underlines match title length exactly
- [ ] No "Is converted" (should be lowercase "is converted")
- [ ] No "internal-only" notes on public traits
- [ ] Cross-reference targets exist (check `.. _label:` definitions)
- [ ] Link targets defined at bottom of file for any new `\`text\`_` links
- [ ] Acronyms expanded on first use

### Step 5: Content Review

- [ ] No internal Amazon references (code.amazon.com, SIM links, CFN,
      SmithyImporter, BackplaneControlService)
- [ ] No `@deprecated` without `message` and `since`
- [ ] Test models use `smithy.example` namespace
- [ ] Test models use `.smithy` IDL format (not `.json`)
- [ ] Javadoc has `@see` links to AWS docs where appropriate

### Step 6: Git Hygiene

```bash
# Verify you're not accidentally including unrelated changes
git diff --cached --stat

# Verify commit message uses imperative mood
git log --oneline -1
```

---

## Quick Fixes for Common Issues

### Spotless Formatting Failed

```bash
./gradlew clean build
# Then stage the reformatted files
git add -u
```

### Changelog Wrong Tense

Find and fix:
```bash
grep -r '"description": "Add ' .changes/next-release/
# Should be "Added" not "Add"
```

### RST Header Too Long

Count the title characters (including backticks and spaces) and make the
underline match exactly. The `.. smithy-trait::` directive line and label
don't count — only the visible heading text matters.

Example: `` ``aws.apigateway#apiKeyRequired`` trait `` = 39 characters
→ underline should be 39 dashes.

### Missing Link Target

If you used `` `Some Text`_ `` in RST, add the target at the bottom:

```rst
.. _Some Text: https://docs.aws.amazon.com/path/to/page.html
```

### GitHub Suggestion Commit Created Duplicates

After pulling Kevin/Manuel's suggestion commits, check for duplicated
headers:

```bash
grep -n "^---" docs/source-2.0/aws/amazon-apigateway.rst | head -20
```

Look for consecutive underlines without content between them.

---

## Integration with Hooks

This validation can be automated as a pre-push hook or a Kiro hook:

```json
{
  "name": "Smithy Pre-Push Validation",
  "version": "1.0.0",
  "when": {
    "type": "userTriggered"
  },
  "then": {
    "type": "runCommand",
    "command": "./gradlew clean build"
  }
}
```

---

## Notes

- `./gradlew clean build` takes ~2.5 minutes on a typical machine
- The docs build requires Python 3.12+ with Sphinx 9.1.0
- Spotless uses the config in `config/spotless/formatting.xml`
- The changelog tool is at `.changes/new-change` (Python script)
- CI runs the same `./gradlew clean build` — if it passes locally, CI
  will pass
