---
name: smithy-docs-navigator
description: >-
  Answer any Smithy question from the authoritative source instead of memory or
  a lossy web-fetch. Use whenever a task involves Smithy - writing, reading,
  reviewing, or fixing a Smithy model; the IDL syntax or grammar; a trait's
  exact rules (@length, @pattern, @range, @required, @http, @paginated, etc.);
  selectors; shapes, resources, operations, or services; HTTP bindings,
  protocols, or auth; whether a model change is backward compatible; or which
  code generator to use for a target language. Reads the published llms.txt
  index and the canonical source file of each page (`.rst` or `.md`), instead
  of a lossy web-fetch. Trigger phrases include "in Smithy", "Smithy model",
  "smithy.io", "what does the Smithy spec say", "how do I define ... in
  Smithy", "is this a breaking change", "which Smithy codegen".
license: Apache-2.0
---

# Smithy docs navigator

## Overview

Smithy's documentation at `https://smithy.io/2.0/` is rendered from
reStructuredText and MyST Markdown sources. Reaching a page through a
summarizing web-fetch is lossy: it returns a paraphrase that silently drops the
exact grammar, trait properties, and normative rules. This skill sends you to
the source file and has you read it with a non-summarizing tool, so every
answer is grounded in source text rather than memory or a summary.

## Usage

Use this skill whenever a request touches Smithy - it is the default way to get
an authoritative answer. Concretely:

- Authoring or editing a model: shape, resource, operation, or service syntax;
  the IDL grammar; mixins; shape-ID resolution.
- A trait's exact rules and properties (e.g. `@length`, `@http`,
  `@paginated`, etc.).
- Selectors: writing or verifying a selector expression.
- HTTP bindings, protocol behavior, or authentication.
- Backward compatibility: whether a change breaks customers (the **Evolving
  Models** guide and the `breakingChanges` rules).
- Which code generator to use for a target language, or where to find examples.
- Any time a web-fetch of a `smithy.io` page came back summarized or truncated
  and you are missing the precise constraint, ABNF, or example.

Do not use this for non-public, organization-internal conventions - only the
published Smithy specification and ecosystem are covered here.

## Core Concepts

- **The source file is authoritative. A summary is not.** The rendered HTML,
  when passed through a summarizing fetch, drops the exact rules you need. Read
  the source.
- **`llms.txt` is the one index.** Published at `https://smithy.io/2.0/llms.txt`,
  its generated documentation links point at each page's canonical `.rst` or
  `.md` source, including resolved symlink targets. It also lists curated
  **Key references** (including backward compatibility), **Examples and
  resources**, **Code generators by language** (with each generator's language
  and status where documented), and **Tooling**. There is no separate map to
  install.
- **Read with a non-summarizing tool.** Routing source text back through a
  summarizing web-fetch loses fidelity just as the HTML did. Use a raw fetch.

## Procedure

1. **Find the page.** Read `https://smithy.io/2.0/llms.txt` (once per session)
   and pick the entry whose title matches your topic. For cross-cutting
   questions use its curated sections: backward compatibility -> **Key
   references**, language codegen -> **Code generators by language**, getting
   started -> **Examples and resources**.

2. **Use the index for a URL you already have.** The source extension may be
   `.rst` or `.md`, and some published pages are symlinks whose canonical source
   path differs from their rendered URL. The index gives the source URL to read.
   For a normal reStructuredText page not present in the index, you can rewrite:

   ```
   https://smithy.io/2.0/<path>.html
       ->  https://raw.githubusercontent.com/smithy-lang/smithy/main/docs/source-2.0/<path>.rst
   ```

   Example: `https://smithy.io/2.0/spec/selectors.html` ->
   `https://raw.githubusercontent.com/smithy-lang/smithy/main/docs/source-2.0/spec/selectors.rst`

3. **Look up a specific trait through the trait index** instead of reading a
   whole spec page. The trait index at `https://smithy.io/2.0/trait-index.html`
   lists every trait and links each one to its defining page and section anchor,
   for example `spec/constraint-traits.html#smithy-api-length-trait`. To answer a
   trait-specific question:

   1. Read the trait index and find the trait's link. The anchor (the part
      after `#`) is derived from the trait's shape ID, so
      `#smithy-api-length-trait` is the `smithy.api#length` trait.
   2. Read the linked source page through the `llms.txt` index. For this
      reStructuredText page, `spec/constraint-traits.html` maps to
      `.../docs/source-2.0/spec/constraint-traits.rst`.
   3. Read that `.rst` source and search for the trait's directive line, which
      names the shape ID directly: `.. smithy-trait:: smithy.api#length`. Read
      from there down to the next `.. smithy-trait::` directive - that span
      (its heading, summary, selector, and members) is the authoritative
      definition of just that one trait, without loading every trait.

   The trait index's own `.rst` source (`trait-index.rst`) is only the
   `.. smithy-trait-index::` build directive, so read the rendered `.html` for
   the index itself, then follow the per-trait links to their `.rst` sources.

4. **Read the full source with a non-summarizing tool** - a shell `curl`, a raw
   file/URL reader - not a summarizing web-fetch. Both `.rst` and `.md` are
   plain text: reStructuredText headings are underlined and code sits in
   `.. code-block:: smithy` blocks; Markdown headings begin with `#`. Their
   cross-reference roles and links name other pages you can resolve through the
   same index.

5. **Ground the answer in what you read.** Quote or cite the specific section.
   If a page cross-references another, resolve that one through the index too
   rather than relying on memory.

## Notes

- The rewrite rule and index target the `2.0` docs on `main`. For another
  version, adjust the `2.0` segment and the branch/tag in the raw URL.
- If a source link 404s, the page may have moved between versions. Re-check the
  index, then fall back to the rendered `.html` page and read it with a
  non-summarizing tool.
