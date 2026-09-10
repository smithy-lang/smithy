# Architecture Decision Records

This directory holds Architecture Decision Records (ADRs) for Smithy. An ADR
captures a single architectural or design decision, the context that forced the
decision, the options that were considered, and the consequences of the choice.
ADRs are immutable once accepted: to change a decision, add a new ADR that
supersedes the old one rather than editing history.

## ADRs vs designs

Smithy already has a top-level `designs/` directory for longer design
narratives (for example a new language feature). ADRs are different in scope:

* An ADR records one decision and its trade-offs in a page or two. It answers
  "why is it built this way, and what did we reject?"
* A design document explores a feature or subsystem in depth, often before
  implementation, and may contain many decisions.

If a change is large enough to need a design document, write one in `designs/`
and, if it also locks in a sharp, reversible-at-a-cost decision, cross-link a
short ADR here.

## When to write an ADR

Write an ADR when a change does any of the following:

* Chooses between multiple viable approaches where the reasons are not obvious
  from the code (for example, picking one concurrency or classloading strategy
  over another).
* Introduces or extends a public extension point (an SPI, a service-provider
  interface, a new trait contract) that others are expected to follow.
* Makes a trade-off that a future reader might otherwise "fix" without knowing
  why it exists (for example, deliberately not caching to avoid a memory leak).
* Establishes a pattern intended to be reused across the codebase.
* Is costly or disruptive to reverse.

## When not to write an ADR

Skip an ADR when:

* The change is a routine bug fix, refactor, or dependency bump with no
  lasting architectural consequence.
* The reasoning is fully evident from the code and its tests.
* The decision is easily reversible and low impact.
* It belongs in user-facing documentation instead. ADRs record internal
  rationale; they are not a substitute for the docs in `docs/`.

When in doubt, prefer a short ADR over none: the cost is low and the record is
most valuable years later when the original context has been forgotten.

## Format

ADRs use the [MADR](https://adr.github.io/madr/) layout. Copy `template.md` to a
new file named `NNNN-short-title.md`, where `NNNN` is the next zero-padded
sequence number (for example `0002-...`). Keep the title short and phrased as
the decision, not the problem.

Required sections:

* Title, status, and date.
* Context and Problem Statement.
* Decision Drivers.
* Considered Options, including the ones that were rejected and why.
* Decision.
* Consequences, both positive and negative, and any trade-off recorded on
  purpose.

Optional but encouraged when relevant:

* A "Pattern for future work" section describing how other code should follow
  the decision, with a pointer to the reference implementation. This is what
  turns a one-off record into a reusable convention.

## Status values

* `Proposed`: under review, not yet agreed.
* `Accepted`: agreed and in effect.
* `Superseded by NNNN`: replaced by a later ADR. Leave the original in place and
  link forward.
* `Deprecated`: no longer relevant, but kept for history.

## Index

* [0001](0001-endpoint-rule-set-classloader-resolution.md) - Resolve endpoint
  rule-set components against the model's classloader.
