# Smithy Agent Skills

This module holds the Smithy [Agent Skills](https://agentskills.io) — plain,
reviewed instruction files that teach AI coding agents how to work with Smithy —
and publishes them as a versioned Maven artifact,
`software.amazon.smithy:smithy-agent-skills`, so consumers can depend on the
skills instead of copying them.

The module has no Java source. The skill files live under
`src/main/resources/skills/`, and the standard Gradle resource pipeline packages
them into the JAR under a top-level `skills/` entry path. Nothing in this module
runs at agent runtime: the artifact is inert content, unpacked by consumers at
build time.

## Available skills

| Skill | What it does |
|-------|--------------|
| [`smithy-docs-navigator`](src/main/resources/skills/smithy-docs-navigator/SKILL.md) | Answers Smithy questions from the authoritative `.rst` docs source instead of a lossy web-fetch. |

## Installing a skill (direct copy)

The skills follow the [Agent Skills](https://agentskills.io) format, so the same
`SKILL.md` works across Claude Code, Codex, Kiro, and other compatible harnesses.
There is no installer to run. Prompt your own agent and have it set the skill up
for you — paste this into any compatible harness:

```
Install the Smithy docs-navigator agent skill for me. Read
smithy-agent-skills/src/main/resources/skills/smithy-docs-navigator/SKILL.md
from the smithy-lang/smithy repository, then copy the
smithy-agent-skills/src/main/resources/skills/smithy-docs-navigator/ folder into
my agent's skills directory for this harness. Show me the file first so I can
review it before you install it.
```

The agent copies the folder into the right place for your harness. If you prefer
to install by hand, the destination differs per harness:

- **Claude Code**: copy the skill folder into `~/.claude/skills/` (personal) or
  `.claude/skills/` (project). It loads immediately with no restart.
- **Codex**: copy the skill folder into `~/.agents/skills/` (user) or
  `.agents/skills/` (repo), then restart Codex.
- **Kiro**: open the *Agent Steering & Skills* panel, choose **Import a skill**,
  then **GitHub**, and point it at the skill's subdirectory
  (`smithy-agent-skills/src/main/resources/skills/smithy-docs-navigator/`), not
  the repository root. You can also copy the folder into `~/.kiro/skills/`
  (global) or `.kiro/skills/` (workspace).

Copying the folder (rather than referencing it live) gives you an inspectable,
pinned copy you have reviewed.

## Adding a skill

1. Add the skill folder under `src/main/resources/skills/<skill-name>/`, with a
   `SKILL.md` following the [Agent Skills](https://agentskills.io) format. In the
   `SKILL.md` frontmatter, set `name` to exactly match the folder name (some
   harnesses require this) and write a `description` of what the skill does and
   when to use it.
2. Add a row to the **Available skills** table above.
3. Keep the skill instruction-only: prose and Markdown, no bundled scripts or
   binaries, and no directive that fetches and executes remote content. Every
   change under `src/main/resources/skills/` requires code-owner review (see
   `.github/CODEOWNERS`); the review is the trust boundary for the content.

The `verifySkillsVerbatim` check runs as part of the module's `check` task and
fails the build if the packaged `skills/` entries differ byte-for-byte from the
source tree, so the artifact always matches the reviewed files.

## Consuming the artifact

Depend on `software.amazon.smithy:smithy-agent-skills`, then unpack its `skills/`
entries at build time. The artifact is packaging only; do not add any code that
discovers, enumerates, or loads skill content from a classpath at runtime.

## Building

```
./gradlew :smithy-agent-skills:build
```

`publishToMavenLocal` stages the JAR and POM for local consumers.
