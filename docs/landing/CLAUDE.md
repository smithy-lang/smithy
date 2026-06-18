# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

This directory (`docs/landing/`) is the **smithy.io landing page** (the site
root `index.html`). It is intentionally a single self-contained static page,
NOT part of the Java/Gradle build of the wider Smithy repo.

## Architecture

- **`index.html`** — the entire page: markup, inline `<style>`, and inline
  `<script>`. There is **no framework, no build step, and no runtime
  dependency**. The only external resource is the Google Fonts stylesheet
  (Schibsted Grotesk + JetBrains Mono) loaded via `<link>`. It replaced a
  prior React/Vite/Storybook app that needed Node to emit this same one file.
- **`assets/`** — brand marks (`smithy-anvil.svg`, `smithy.svg` dark-text,
  `smithy-dark.svg` light-text), language logos (`icons/`), favicons, and
  `og-cover.png` (the social share image). Referenced with **root-relative**
  paths (e.g. `assets/icons/ts.svg`) because the page is served from the site
  root after the build copies it there.
- **`og-cover-source.html`** — an Ember-themed 1200×630 HTML card that is
  screenshotted to produce `assets/og-cover.png`. Not shipped to the site.
- **`README.md`** — human setup + OG-image regeneration steps.

### How it ships
`docs/Makefile`'s `merge-versions` target copies `landing/index.html` and
`landing/assets/` into `build/html/` so this page becomes the site root
`index.html`, alongside the versioned docs (`/2.0/`, `/1.0/`) and `root/`
pages. `README.md`, `CLAUDE.md`, and `og-cover-source.html` are deliberately
NOT copied.

### Theming (single source of truth)
The design system (colors, type, spacing, tokens, and how to propagate the
theme to the docs site) is documented in `BRAND.md` — update that doc first when
the brand evolves, then mirror the token values into the page.

All color lives in exactly two CSS custom-property blocks:
`html[data-mode="dark"]{…}` and `html[data-mode="light"]{…}`. Everything else
references `var(--token)` — never hardcode a color outside those two blocks.
Token vocabulary is documented in a comment at the top of the `<style>`.
The theme is **Ember**: warm charcoal (dark) / warm parchment (light) with
Smithy red as the single saturated accent.

The inline `<head>` script sets `data-mode` before paint (no FOUC):
saved `localStorage['smithy-mode']` wins, else the OS `prefers-color-scheme`.
A live `matchMedia` listener follows OS changes until the user clicks the
toggle (which sets the explicit override). `syncTheme()` is the one function
that reconciles DOM state (wordmark `src`, `aria-pressed`, label) on boot,
toggle, and OS-change.

## Local development

Serve the directory (root-relative asset paths require a server, not `file://`):

```
cd docs/landing && python3 -m http.server 8788   # then open http://localhost:8788/
```

To preview the full site as deployed (landing + docs), build from `docs/`:
`make html && make serve` (requires `make install` once; Python/Sphinx only).

## Invariants — do not break when adding features

- **Single self-contained file.** No framework, no build step, no npm. Inline
  `<style>`/`<script>` only; Google Fonts `<link>` is the sole external dep.
- **Code/terminal/diff panels stay DARK in both light and dark mode.** Inside
  those panels use the mode-stable `--code-*` / `--tok-*` / `--code-accent`
  tokens, never the page accent tokens (which flip per mode and fail AA on the
  dark panel in light mode).
- **No em-dashes/en-dashes in visible copy** (CSS comments are fine). Voice
  matches the Smithy 2.0 docs: clear, declarative, technical, not hypey;
  "type-safe", "backward compatibility".
- **No invented facts.** No fabricated metrics, customer logos, or quotes. The
  SiriusXM quote is the only testimonial. True claims only (open source,
  Apache-2.0, used at AWS, multi-language). Technical examples must match the
  `Weather` service in `../source-2.0/` (e.g. `@paginated` is an operation
  trait; the `City` resource has only `read`/`list`).
- **Buttons: solid fills only; links: solid color** (no gradient buttons/links).
  Ambient background glows are fine.
- **Accessibility (WCAG 2.1 AA, both modes):** skip link, `:focus-visible`
  rings, single `<h1>` + ordered headings, real `<button>` toggle with
  `aria-pressed`, mobile hamburger with `aria-expanded`/`controls`/Esc/focus,
  `prefers-reduced-motion` disables all motion, ≥44px tap targets, 200% zoom.
- **Reveal-on-scroll is gated behind `html.js`** (added by the head script,
  skipped under reduced-motion) so content is visible without JS.
- **Responsive 320–2560px**, no horizontal overflow; code panels scroll
  internally.
- **Every `href`/`src` must resolve.** Internal docs use absolute `/2.0/…` and
  `/implementations.html`; external links use `target="_blank" rel="noopener"`.
  The valid link set is the nav + footer + language-repo list already present;
  keep nav, mobile menu, and footer hrefs in sync.

## Language links
Each language logo (fan-out chips + Clients/Servers pills) links to its codegen
repo: TS/Python/Rust/Kotlin/Swift/Ruby/Java/Dafny → `smithy-lang/smithy-*`,
Go → `aws/smithy-go`, Scala → `disneystreaming/smithy4s`. Keep the fan-out and
showcase orders consistent.

## Regenerating the OG image
Edit `og-cover-source.html`, serve this dir, screenshot it at a **1200×630**
viewport, and overwrite `assets/og-cover.png` (keep it PNG, exactly 1200×630).
