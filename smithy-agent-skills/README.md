# Smithy Agent Skills

This module holds the Smithy [Agent Skills](https://agentskills.io) — plain,
reviewed instruction files that teach AI coding agents how to work with Smithy —
and publishes them as a versioned Maven artifact,
`software.amazon.smithy:smithy-agent-skills`, so consumers can depend on the
skills instead of copying them.

The module has no Java source. The skill files live under
`src/main/resources/skills/`, organized into component subdirectories that
mirror the [smithy.io](https://smithy.io) documentation layout — a `core/` tree
for the IDL and specification, plus one subtree per Smithy component whose skills
this module hosts (`typescript/`, `java/`, and so on). The standard Gradle
resource pipeline packages the whole tree into the JAR under a top-level
`skills/` entry path. Nothing in this module runs at agent runtime: the artifact
is inert content, unpacked by consumers at build time.

### Directory layout and skill names

The component subdirectory is **organizational only** — it groups skills for
readers and gives each component a directory that code owners can scope reviews
to. It does *not* namespace the skill. Per the Agent Skills format a skill is
identified by its `name` frontmatter, which must equal the skill's **immediate
parent directory** (the leaf folder), not the path beneath `skills/`. Because
`name` is therefore a flat, global identifier, skills in this module follow the
convention **`smithy-<component>-<capability>`** (for example
`smithy-docs-navigator` under `core/`, `smithy-ts-<capability>` under
`typescript/`). The convention — not the directory — is what keeps names from
colliding when these skills are composed alongside other skill sets downstream.

```
src/main/resources/skills/
├── core/
│   └── smithy-docs-navigator/
│       └── SKILL.md
├── typescript/
│   └── smithy-ts-<capability>/
│       └── SKILL.md
└── java/
    └── smithy-java-<capability>/
        └── SKILL.md
```

## Available skills

| Skill | Component | What it does |
|-------|-----------|--------------|
| [`smithy-docs-navigator`](src/main/resources/skills/core/smithy-docs-navigator/SKILL.md) | `core` | Answers Smithy questions from the authoritative `.rst` and `.md` docs sources instead of a lossy web-fetch. |

## Working with agents

1. Ask your agent to install the Smithy docs-navigator skill from this
   repository.
2. Ask it to use the skill for its next Smithy task.

A good generic prompt is:

```
Install and use the Smithy docs-navigator skill. Read
smithy-agent-skills/src/main/resources/skills/core/smithy-docs-navigator/SKILL.md
from the smithy-lang/smithy repository, then copy the
smithy-agent-skills/src/main/resources/skills/core/smithy-docs-navigator/ folder into
this harness's skills directory. Review SKILL.md before copying it.
```

The skills follow the [Agent Skills](https://agentskills.io) format, so the same
folder works across Claude Code, Codex, Kiro, and other compatible harnesses.
For a manual installation, the destination differs per harness:

- **Claude Code**: copy the skill folder into `~/.claude/skills/` (personal) or
  `.claude/skills/` (project). Claude Code detects changes automatically; restart
  only if the top-level skills directory did not already exist.
- **Codex**: copy the skill folder into `~/.agents/skills/` (user) or
  `.agents/skills/` (repo). Codex detects changes automatically; restart if the
  skill does not appear.
- **Kiro**: open the *Agent Steering & Skills* panel, select **+**, choose
  **Import a skill**, then **GitHub**, and point it at the skill's subdirectory
  (`smithy-agent-skills/src/main/resources/skills/core/smithy-docs-navigator/`),
  not the repository root. GitHub import supports public repositories. You can
  also copy the folder into `~/.kiro/skills/` (global) or `.kiro/skills/`
  (workspace); imported skills work
  immediately.

Copying the folder (rather than referencing it live) gives you an inspectable,
pinned copy you have reviewed.

1. Add the skill folder under the appropriate component subdirectory —
   `core/<skill-name>/` for IDL and specification skills, or
   `<component>/<skill-name>/` (`typescript/`, `java/`, ...) for a component's
   skills. Give it a `SKILL.md` following the
   [Agent Skills](https://agentskills.io) format. In the frontmatter, set `name`
   to the `smithy-<component>-<capability>` convention — it must exactly match
   the leaf folder name — and write a `description` of what the skill does and
   when to use it.
2. Add a row to the **Available skills** table above.
3. Every change under `src/main/resources/skills/` requires code-owner review
   (see `.github/CODEOWNERS`); the review is the trust boundary for the content.
   Scoping `CODEOWNERS` by component subdirectory lets the team closest to a
   component review its skills, even though everything ships from this one
   module on the Smithy release.

### Bundled supporting files (`references/`, `scripts/`, `assets/`)

The Agent Skills format is not limited to a lone `SKILL.md`. A skill directory
may also carry supporting files that the agent loads *on demand* (progressive
disclosure), and this module supports them:

```
core/smithy-docs-navigator/
├── SKILL.md              # required: metadata + instructions
├── references/           # optional: detailed docs the agent reads when needed
├── assets/               # optional: templates, schemas, data files
└── scripts/              # optional: executable helpers (see policy below)
```

- **`references/`** and **`assets/`** — fully supported and encouraged. Keep the
  main `SKILL.md` focused (the format recommends under ~500 lines) and move
  detailed material into `references/` so it loads only when a task needs it.
  Reference other files with **relative paths from the skill root, one level
  deep** (for example `See [the grammar reference](references/idl-grammar.md)`),
  as the format specifies.
- **`scripts/`** — permitted, but held to a higher bar because executable code is
  a larger review surface than prose. A bundled script must be self-contained or
  clearly document its dependencies, must not fetch-and-execute remote content,
  and must map to a capability the reviewer can verify (prefer thin wrappers over
  the public Smithy CLI over novel logic). Declare any environment requirements
  in the `SKILL.md` `compatibility` frontmatter field (for example
  `compatibility: Requires the Smithy CLI`). Code owners review bundled scripts
  as carefully as the instructions.
- **No fetch-and-execute directives** in any file: nothing may download and run
  remote content at agent runtime. This is the one hard line the format leaves to
  the publisher, and this module draws it.

`verifySkillsVerbatim` walks the entire skill tree recursively, so every bundled
`references/`, `assets/`, and `scripts/` file is packaged byte-for-byte and is
covered by the same guarantee as `SKILL.md` — the check runs as part of the
module's `check` task and fails the build if the packaged `skills/` entries
differ from the reviewed source. Nothing here changes because a skill has
supporting files; the recursive walk already accounts for them.

## Consuming the artifact

Depend on `software.amazon.smithy:smithy-agent-skills`, then unpack its `skills/`
entries at build time. The artifact is packaging only; do not add any code that
discovers, enumerates, or loads skill content from a classpath at runtime.

## Building

```
./gradlew :smithy-agent-skills:build
```

`publishToMavenLocal` stages the JAR and POM for local consumers.
