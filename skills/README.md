# Smithy Agent Skills

This directory holds [Agent Skills](https://agentskills.io) that teach AI coding
agents how to work with Smithy. Each skill is a self-contained folder with a
`SKILL.md` file:

```
skills/
└── smithy-docs-navigator/
    └── SKILL.md
```

Skills here are **plain, reviewed instruction files**: no executable code, no
runtime downloads. An agent only ever uses a skill after you deliberately
install it into your own agent's skills directory. Nothing is bundled into a
build artifact or distributed to you automatically.

## Available skills

| Skill | What it does |
|-------|--------------|
| [`smithy-docs-navigator`](smithy-docs-navigator/SKILL.md) | Answers Smithy questions from the authoritative `.rst` docs source instead of a lossy web-fetch. |

## Installing a skill

These skills follow the [Agent Skills](https://agentskills.io) format, so the
same `SKILL.md` works across Claude Code, Codex, Kiro, and other compatible
harnesses. There is no installer to run. Instead, you prompt your own agent and
have it set the skill up for you. Paste this prompt into your agent (Claude
Code, Codex, Kiro, or any compatible harness):

```
Install the Smithy docs-navigator agent skill for me. Read
skills/smithy-docs-navigator/SKILL.md from the smithy-lang/smithy repository,
then copy the skills/smithy-docs-navigator/ folder into my agent's skills
directory for this harness. Show me the file first so I can review it before
you install it.
```

The agent copies the folder into the right place for whichever harness you are
running. The destination differs per harness, so if you prefer to install by
hand:

- **Claude Code**: copy the skill folder into `~/.claude/skills/` (personal) or
  `.claude/skills/` (project). It loads immediately with no restart needed.
- **Codex**: copy the skill folder into `~/.agents/skills/` (user) or
  `.agents/skills/` (repo), then restart Codex.
- **Kiro**: open the *Agent Steering & Skills* panel, choose **Import a skill**,
  then **GitHub**, and point it at the skill's subdirectory
  (`skills/smithy-docs-navigator/`), not the repository root. You can also copy
  the folder into `~/.kiro/skills/` (global) or `.kiro/skills/` (workspace).

Copying the folder (rather than referencing it live) means you get an
inspectable, pinned copy you have reviewed.

## Adding a new skill

1. Create a folder `skills/<skill-name>/` and put a `SKILL.md` inside it.
2. Keep the skill **instruction-only**: prose and Markdown, no bundled scripts
   or binaries, and no directive that fetches and executes remote content. This
   keeps every skill fully reviewable in a pull request.
3. In the `SKILL.md` YAML frontmatter, set `name` to exactly match the folder
   name (some harnesses require this) and write a `description` that says what
   the skill does and when to use it.
4. Add a row to the table above.

Because these are ordinary files in the repository, every change to a skill goes
through normal pull-request review before it can reach anyone. The review is the
trust boundary.
