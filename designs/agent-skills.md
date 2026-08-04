# Smithy Agent Skills

* **Authors**: yasmewad
* **Created**: 2026-07-27
* **Last updated**: 2026-07-29

## Abstract

This proposal adds a set of reviewable agent skills to the Smithy repository
and publishes them as the Maven artifact
`software.amazon.smithy:smithy-agent-skills`. A skill is a folder of plain
Markdown instructions in the [Agent Skills format](https://agentskills.io/)
that teaches an AI coding agent how to work with Smithy accurately. The first
skill, `smithy-docs-navigator`, directs agents to the canonical documentation
source for a question, and to read it with a non-summarizing tool, rather than
answering from stale training data or a summarized web fetch.

## Motivation

AI coding agents are increasingly the first consumer of Smithy documentation.
Left to their own devices they answer from training data that lags releases, or
from web fetches that drop the version-specific details that matter (trait
constraints, selector grammar, backward-compatibility rules). The failure mode
differs by harness: Claude Code summarizes the page instead of returning it, and
Kiro fetches full HTML only after a web search that may miss the right page.
Either way the agent rarely lands on the exact source text, and the result is
plausible but wrong guidance a user cannot easily catch.

The documentation itself is already agent-friendly. The website publishes an
`llms.txt` index, and the source `.rst` and `.md` pages are the complete,
canonical text of the specification. What is missing is the connective
instruction layer: a small, reviewable document that tells an agent when and
how to use those sources.

Distributing that layer as repository files alone leaves it hard to depend on.
Anyone who wants to track skill updates over time has no version to pin to and
bump when a skill changes, and a downstream build that wants to redistribute the
skills (say, packaging them into a larger toolkit) has no artifact to depend on
at all. It has to vendor a copy of the repository or scrape files from GitHub at
build time.

## Background

Agent skills are instruction content: loading a skill can change the actions an
agent chooses, including which files it reads, which tools it invokes, and which
bundled scripts it runs. This makes them a different kind of risk than code. A
library with a vulnerability misbehaves in ways tests and scanners can catch. A
skill with an injected instruction steers an agent while looking like ordinary
documentation. So although the artifact proposed here is inert, its contents
belong to the consumer's instruction supply chain and need an explicit trust
boundary.

This puts two demands on the solution about trust. A consumer must be able to
trace any skill to the exact dependency, build step, version, and reviewed
change that introduced it, with nothing reaching an agent through an unaudited or
transitive path. And what ships must be exactly what was reviewed.

Meeting both tells a consumer where each skill came from and that it arrived
unchanged. Neither makes a skill correct or safe on its own. Content quality and
behavior stay with code-owner review and any policy checks or behavioral
evaluations layered on top. The proposal keeps those two concerns, distribution
and content quality, separate.

## Goals and non-goals

**Goals:** the design solves three problems, one per stakeholder.

1. **Let a Smithy user give their agent accurate Smithy knowledge.** A user of
   any compatible harness (Claude Code, Codex, Kiro, and other adopters of the
   open Agent Skills format) can add a Smithy skill (by copying the folder or
   consuming the artifact) and have their agent answer from the authoritative
   published documentation instead of stale training data or a lossy web fetch.
2. **Let the Smithy team author and vend that knowledge as reviewed content.**
   The team can define skills as instruction content (plus optional bundled
   `references/`, `assets/`, or `scripts/`) in the repository, where every
   change goes through code-owner review, so what an agent loads is Smithy's
   vetted guidance rather than whatever a user pieced together.
3. **Let downstream builds depend on the skills, not copy them.** The skills are
   published as a versioned Maven artifact so a consuming build can pin and
   depend on them the way it depends on any other Smithy artifact, and receives
   byte-identical content to what was reviewed, with no unversioned snapshot to
   vendor and no files to scrape from GitHub.

**Non-goals:**

1. **A comprehensive Smithy skill catalog.** This establishes the module, the
   naming and layout conventions, and the first skill (`smithy-docs-navigator`).
   Further skills are authored later as separate reviewed changes.
2. **A guarantee of correct agent output.** The skill improves which source an
   agent reads. It does not control what the agent concludes: answer quality
   still depends on the model and the harness, and one whose only retrieval tool
   summarizes cannot fully honor the skill.
3. **Certifying per-harness behavior.** The design targets the portable Agent
   Skills format. How faithfully each harness activates, loads, and retrieves a
   skill is the harness's concern, and this proposal does not test or certify it.
4. **Keeping the retrieved documentation current.** The skill points at live
   docs, so a copy shipped in one release can read documentation that has since
   changed. Versioning the artifact does not version what it retrieves.
   Release-pinned sources are follow-up work on the skill content.
5. **Managing skills on a user's machine (install, update, removal).** Placing
   and updating skills is the harness's lifecycle to own. Smithy vends reviewed
   content and a dependable artifact and leaves that lifecycle to the harness or
   the user.

## Proposal

### The skills

The skills follow the [Agent Skills format](https://agentskills.io/), portable
across harnesses (Claude Code, Codex, Kiro, and other adopters), so one reviewed
source serves every harness. The first skill, `smithy-docs-navigator`, fixes
*source selection*: it points the agent at the authoritative page through the
`llms.txt` index. It also asks the agent to read that page with a non-summarizing
tool, which improves *transport fidelity* but depends on the harness having one.
A single exploratory A/B run (n=1) showed a large drop in agent credits, wall-clock
time, and tool turns. That measures efficiency, not answer quality, but the
mechanism is simple: an agent that knows where the index lives stops searching
for it.

Skills live in a dedicated content-only Gradle module,
`smithy-agent-skills`, under its standard resource directory, organized into
component subdirectories that mirror how the Smithy documentation site is laid
out (a `core/` tree for the IDL and specification, and one subtree per Smithy
component whose skills this repository hosts):

```
smithy-agent-skills/
├── README.md                                    # catalog + install instructions (not packaged)
├── build.gradle.kts
└── src/main/resources/skills/
    ├── core/
    │   └── smithy-docs-navigator/
    │       ├── SKILL.md                          # required: YAML frontmatter + Markdown instructions
    │       ├── references/                       # optional: extra docs the agent reads on demand
    │       ├── scripts/                          # optional: executable helpers (e.g. a validation script)
    │       └── assets/                           # optional: templates, schemas, data files
    ├── typescript/
    │   └── smithy-ts-<capability>/
    │       └── SKILL.md
    └── java/
        └── smithy-java-<capability>/
            └── SKILL.md
```

A skill is a folder with a required `SKILL.md` and the three optional sibling
directories shown above. `SKILL.md` is YAML frontmatter plus a Markdown body.
The frontmatter carries `name` (the flat, ≤64-char identifier that must match
the leaf folder) and `description` (what the skill does and when to use it, read
at startup to decide relevance), plus optional `license` and `compatibility`
(environment needs, e.g. "requires the Smithy CLI"). The format loads these
progressively: frontmatter at startup, the body on activation, sibling files
only when a task needs them.

**Directory layout and naming.** The component subdirectories (`core/`,
`typescript/`, `java/`, ...) only scope reviews and group skills for readers.
They do *not* namespace the skill. The format requires a skill's `name` to equal
its immediate parent folder, not the path, so the name is a flat global
identifier that must disambiguate on its own once composed with other sets
downstream. Every name therefore carries a `smithy-` prefix, and a component
skill carries its component too (`smithy-ts-<capability>`,
`smithy-java-<capability>`). Core skills name the capability directly
(`smithy-docs-navigator`), since they document the IDL rather than a component.
Tools
that discover skills by walking the tree handle the nested layout without change.

**Bundled supporting files.** Every file in a skill is reviewed content, because
a `references/` file or `assets/` template steers the agent as much as the
instructions. `references/` and `assets/` are encouraged (move detail out of
`SKILL.md`, referenced one level deep per the format) and reviewed like the
rest. `scripts/` get the closest scrutiny, reviewed for what the code can do
(network, subprocesses, filesystem writes, credentials) and kept to thin
wrappers over the public Smithy CLI. One rule is absolute: no file may fetch and
execute remote content at runtime, since that is the one behavior review cannot
catch, running whatever a remote endpoint serves after the diff was approved.

Users who want a skill without any build tooling copy the folder into their
harness's skills directory (`~/.claude/skills/`, `~/.agents/skills/`,
`~/.kiro/skills/`, or the project-local equivalents). The module README
documents the copy prompt and per-harness destinations. Copying gives the
user an inspectable, pinned copy they have reviewed. This path is unchanged by
this proposal and remains fully supported.

### The artifact

The module publishes `software.amazon.smithy:smithy-agent-skills`, versioned
with each Smithy release like every other publication, so it inherits the
existing release process (publishing, signing, versioning) unchanged with no new
pipeline to maintain. The module contains no Java source and declares no
dependencies. Gradle's standard resource pipeline packages the `skills/` tree
into the JAR under a top-level `skills/` entry path with no custom copy wiring,
so the artifact is a zip of the exact reviewed source files.

A `verifySkillsVerbatim` task, wired into `check`, walks the source skills
tree and the built JAR's `skills/` entries and fails the build on any missing
entry, any unexpected extra, or any byte-content mismatch. The verbatim
property is checked on every build rather than asserted in documentation. The
packaged tree holds only the component subdirectories and the skill folders
within them. The catalog and install documentation live in the module-root
README, outside the resource root, so they never ship as artifact content.

Consumers of the artifact unpack the `skills/` entries at build time into
whatever layout their tooling expects. Nothing should load skill content from
a JAR on a classpath at agent runtime. The artifact is packaging for
build-time consumption, and does not act as a runtime mechanism.

### Two separate guarantees: packaging integrity and content review

`verifySkillsVerbatim` is a *packaging-integrity* check: the bytes a consumer
unpacks are identical to the reviewed source at that release. It cannot prove
those bytes are accurate or safe. That is *content review*'s job, human
code-owner review with the same limits it has for any change. The check just
means a reviewer only has to trust the diff in front of them, since the artifact
cannot later ship anything that diff did not contain. So the promise is bounded:
a consumer gets exactly what the team reviewed, not a guarantee that what was
reviewed is correct or safe.

## Alternatives and trade-offs

The alternatives fall into two groups: how skills are *distributed* (the first
five), and choices about how the skills are *structured* within the proposal
(the last two).

### Distribution

#### Runtime classpath discovery

Stage skills under a well-known resource path (for example `META-INF/skills/`)
and discover them at agent runtime by scanning the classpath via
`ClassLoader.getResources()`, with a CLI command to install what was found.

Pros:

* Fully automatic: a consumer adds the dependency and the skills appear with no
  unpack step.

Cons:

* Runtime scanning defeats the attribution requirement. The scan picks up skill
  content from any JAR on the classpath, transitively and without re-review, so
  a consumer can no longer name the dependency, build step, and reviewed change
  behind a skill.
* Careful repository hygiene does not help, because the scanning mechanism is
  what opens the hole regardless of how clean any individual JAR is.

Rejected. The problem is the runtime scan, not the artifact itself, so this
proposal includes no runtime discovery, CLI installer, or classpath mechanism of
any kind.

#### Plain files only, no artifact

Ship the skill folders in the repository and stop there.

Pros:

* Zero build machinery, and works today for individual users who copy a folder.

Cons:

* Leaves downstream redistributors with no versioned dependency. They must
  vendor a repository snapshot that starts going stale immediately, or fetch
  files from GitHub inside their builds, trading a reviewed artifact for a
  network dependency on a moving branch.

Rejected only as the sole path. Publishing the artifact removes both workarounds
at the cost of one content-only module with no code, and the plain-files path
remains fully supported alongside it, so the artifact adds a capability without
taking one away.

#### Fetch skills from a repository on demand, the way `smithy init` fetches templates

Add a command that clones a skills repository on demand and copies a chosen
skill into the harness's skills directory, mirroring how `smithy init` clones a
templates repository and copies the requested template into a user's project.

Pros:

* A good fit for the individual-user path: explicit, user-run, and no build
  integration needed.
* Smithy already distributes non-library content this way, so the pattern is
  proven.

Cons:

* Does not solve the problem this design is about. A fetch pulls from a moving
  branch with no version pinning, so a consumer cannot depend on a specific
  reviewed revision.
* A network fetch inside a build trades a reproducible, pinned artifact for a
  live dependency on whatever the branch holds at build time.

A fetch and the artifact solve different problems. An on-demand fetch is a fine
way for a person to grab a skill, but a downstream build needs the versioned
artifact to depend on skills the way it depends on `smithy-model`. This design
specifies the artifact and leaves any fetch-style convenience command as
separate future work.

#### Publish documentation improvements only

Point agents at the existing `llms.txt` index and raw documentation sources ad
hoc, without a skill.

Pros:

* No new artifact, since the documentation sources already exist.

Cons:

* Without a skill, each user hand-writes their own instructions of varying
  quality. The efficiency gain observed in the A/B run comes from the reviewed
  instruction layer doing the pointing consistently, which ad hoc guidance does
  not.

Documentation improvements and skills complement each other rather than
substitute. The skill is the reviewed instruction layer that points at the
improved docs.

#### A separate repository for skills

Publish from a dedicated `smithy-agent-skills` repository rather than a module
inside `smithy-lang/smithy`.

Pros:

* Isolates the content and its review rules.
* Gives skills an independent release cadence, which is the *correct* home for a
  skill that documents a component with its own release train (a Rust codegen
  skill tracking smithy-rs rather than core).

Cons:

* For a skill about the core IDL, accuracy is release-relative to core, so
  versioning it *with* the release is a feature. A separate repository breaks
  that coupling for no gain.
* Fragments review across two places while the skill count is small.
* Has to stand up its own release infrastructure from scratch: artifact
  publishing to Maven Central, signing and credential setup, release CI and
  versioning automation, and the branch-protection and ownership configuration
  every published repository needs. A module inside `smithy-lang/smithy` inherits
  all of this from the release process that already ships every Smithy artifact
  and adds only a content-only module to it. Standing up and maintaining a
  parallel publishing pipeline is a large recurring cost for what is today a
  directory of Markdown.

Rejected for now, though the door stays open. The component subdirectory already
in the proposal is the resolution: core skills live in `core/` and version with
core, and a per-component subdirectory is the extraction seam if a component's
skills ever need their own cadence, since that subtree can move to the
component's own repository keeping the same naming convention and module pattern.
A component that takes on its skills is already publishing its own artifacts, so
it absorbs the skills into a release pipeline it already runs instead of standing
up a new one.

#### Per-harness marketplaces or registries

Publish into the skill registries or marketplaces that some harnesses provide.

Pros:

* Native discovery and install for users of that harness.

Cons:

* Reaches users of that harness only.
* Multiplies the number of places the content must be kept current.
* Puts a third party's review process between the Smithy team and its published
  instructions.

Rejected. The Agent Skills format is portable by design, so one reviewed source
can serve every harness.

### Structure within the proposal

#### Flat skill layout, no component subdirectories

Place every skill directly under `skills/<name>/`, with no `core/`,
`typescript/`, or `java/` grouping.

Pros:

* Simpler while there is one skill, and it is the layout the format documents
  most often.

Cons:

* No per-component review boundary.
* No home that matches where a reader expects a component's material to live.

Chosen: group by component. The grouping lets `CODEOWNERS` scope review by
component and mirrors the documentation site's own layout, and it costs nothing
at the format level, since identity comes from the `name` frontmatter (which
must match the leaf folder) rather than the path, and tools that walk the tree
discover skills at any depth. The grouping does not create a namespace, which is
why the skill name carries its own disambiguating prefix regardless of directory.

#### Instruction-only skills, no bundled scripts

Forbid `scripts/` (and even `references/` and `assets/`) and allow only a lone
`SKILL.md` of prose.

Pros:

* Draws the security line in the simplest possible place: no executable content
  ships, so there is nothing to review beyond instructions.

Cons:

* Gives up capability the Agent Skills format is built to provide. A skill that
  can bundle a small validation script or a schema is more useful than one that
  can only describe the steps in prose.
* Progressive disclosure exists precisely so supporting files load only when a
  task needs them, and forbidding them wastes that mechanism.

Chosen: allow bundled files under review, on the review-and-prohibition terms
set out under "Bundled supporting files" in the Proposal.

## Future work

### Harness-native plugin vending

Several harnesses can install a repository directly as a plugin. Claude Code
reads a top-level `.claude-plugin/marketplace.json` plus `.claude-plugin/plugin.json`
and installs with `/plugin install <name>@<marketplace>` (the pattern AWS
[agent-toolkit-for-aws](https://github.com/aws/agent-toolkit-for-aws) uses).
Codex and Cursor have analogous manifests. Adding these would let a user install
every Smithy skill with one command.

Deferred, because it is a separate delivery vehicle for the same reviewed files
and it carries one layout choice to make deliberately: Claude Code's plugin
loader wants skills flat under `skills/`, but this design groups them by
component. The plugin work resolves that (a manifest `skills` array of the
component directories, or a flatten step) when it happens.

### Release-pinned documentation sources

`smithy-docs-navigator` points at live docs (see non-goal 4), so a skill shipped
in one release can read documentation that has since changed. Pinning the
skill's URLs to a release tag, so the retrieved content matches the release the
skill shipped in, is a follow-up change against the skill content itself.

## FAQ

### Does the artifact run anything?

No. The module has no Java source, no dependencies, and no service-provider
registrations. The JAR contains Markdown files under `skills/` and standard
manifest entries. Consumers unpack it at build time, and nothing loads it at
runtime.

### Doesn't publishing a JAR of skills create a classpath injection risk?

No. The injection risk in classpath-based skill delivery comes from the
unrestricted runtime search, not from the existence of an artifact. Here there
is no search: a consumer names this one artifact explicitly, extracts it in
their own build, and reviews what arrives like any other dependency. Bundling
happens explicitly at build time, and nothing anywhere scans for skill content.

### Why version the skills with Smithy releases?

Mainly because it costs nothing: the skills live in the repository, so the
existing release process publishes them with no new machinery, and a consumer
pins and bumps them the way it pins every other Smithy artifact. The version
tracks which reviewed skill files a consumer has. It says nothing about the
accuracy of the documentation those files point at, since the non-goals are
explicit that the artifact version does not version the retrieved content.

### Are bundled scripts allowed, and what stops fetch-and-execute directives?

Bundled `scripts/` are allowed under a higher review bar (see "The skills"), and
fetch-and-execute of remote content at runtime is prohibited outright. What
stops a dangerous script is code-owner review reading the diff. The
`verifySkillsVerbatim` check does not help here: it only guarantees the shipped
bytes match the reviewed bytes, so a prohibited file cannot slip in without
appearing in a reviewed diff, but the review is what has to notice it there.

### How do I add a new skill?

Add a folder under the appropriate component subdirectory in
`smithy-agent-skills/src/main/resources/skills/` (`core/` for IDL and
specification skills, `typescript/`, `java/`, and so on for
component-documentation skills). Give it a `SKILL.md` whose frontmatter `name`
carries the `smithy-` prefix (and the component too, for a
component-documentation skill) and matches the leaf folder name, add any bundled
`references/`/`assets/`/`scripts/` the skill needs
(see "The skills" for the review bar), add a row to the README's catalog
table, and raise a pull request. Code owners review it like any other change.
Scoping `CODEOWNERS` by component subdirectory lets the team most familiar with
a component review its skills.
