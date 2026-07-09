---
name: smithy-docs-navigator
description: Answer any Smithy question from the authoritative source instead of memory or a lossy web-fetch. Use whenever a task involves Smithy - writing, reading, reviewing, or fixing a Smithy model; the IDL syntax or grammar; a trait's exact rules (@length, @pattern, @range, @required, @http, @paginated, etc.); selectors; shapes, resources, operations, or services; HTTP bindings, protocols, or auth; whether a model change is backward compatible; or which code generator to use for a target language. Reads the published llms.txt index and the reStructuredText (.rst) source of each page - the complete, version-matched text a summary would drop. Trigger phrases include "in Smithy", "Smithy model", "smithy.io", "what does the Smithy spec say", "how do I define ... in Smithy", "is this a breaking change", "which Smithy codegen".
license: Apache-2.0
---

# Smithy docs navigator

## Overview

Smithy's documentation at `https://smithy.io/2.0/` is rendered from
reStructuredText sources. Reaching a page through a summarizing web-fetch is
lossy: it returns a paraphrase that silently drops the exact grammar, trait
properties, and normative rules. This skill sends you to the `.rst` source and
has you read it with a non-summarizing tool, so every answer is grounded in the
complete, version-matched text rather than memory or a summary.

## Usage

Use this skill whenever a request touches Smithy - it is the default way to get
an authoritative answer. Concretely:

- Authoring or editing a model: shape, resource, operation, or service syntax;
  the IDL grammar; mixins; shape-ID resolution.
- A trait's exact rules and properties (`@length`, `@pattern`, `@range`,
  `@required`, `@http`, `@paginated`, protocol and auth traits, ...).
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

- **The `.rst` source is authoritative; a summary is not.** The rendered HTML,
  when passed through a summarizing fetch, drops the exact rules you need. Read
  the source.
- **`llms.txt` is the one index.** Published at `https://smithy.io/2.0/llms.txt`,
  its links already point at each page's `.rst`. Beyond the spec pages it lists
  curated **Key references** (including backward compatibility), **Examples and
  resources**, **Code generators by language** (with each generator's language
  and maturity), and **Tooling**. There is no separate map to install.
- **Read with a non-summarizing tool.** Routing the `.rst` back through a
  summarizing web-fetch loses fidelity just as the HTML did. Use a raw fetch.

## Procedure

1. **Find the page.** Read `https://smithy.io/2.0/llms.txt` (once per session)
   and pick the entry whose title matches your topic. For cross-cutting
   questions use its curated sections: backward compatibility -> **Key
   references**, language codegen -> **Code generators by language**, getting
   started -> **Examples and resources**.

2. **Or rewrite a URL you already have** instead of consulting the index:

   ```
   https://smithy.io/2.0/<path>.html
       ->  https://raw.githubusercontent.com/smithy-lang/smithy/main/docs/source-2.0/<path>.rst
   ```

   Example: `https://smithy.io/2.0/spec/selectors.html` ->
   `https://raw.githubusercontent.com/smithy-lang/smithy/main/docs/source-2.0/spec/selectors.rst`

3. **Read the full source with a non-summarizing tool** - a shell `curl`, a raw
   file/URL reader - not a summarizing web-fetch. The `.rst` is plain text:
   headings are underlined, code sits in `.. code-block:: smithy` blocks, and
   `:ref:` / `:doc:` roles name other pages you can resolve through the same
   index.

4. **Ground the answer in what you read.** Quote or cite the specific section.
   If a page cross-references another, resolve that one through the index too
   rather than relying on memory.

## Notes

- The rewrite rule and index target the `2.0` docs on `main`. For another
  version, adjust the `2.0` segment and the branch/tag in the raw URL.
- If a `.rst` source 404s, the page may have moved between versions; fall back
  to the rendered `.html` page and read it with a non-summarizing tool.
