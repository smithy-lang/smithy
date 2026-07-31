# Smithy landing page

The smithy.io landing page (site root `index.html`).

This is a **single self-contained static page** — `index.html` with inline CSS
and JS, plus `assets/` (brand marks, language icons, favicons). There is **no
build step, no framework, and no npm dependency**. The only external resource is
the Google Fonts stylesheet loaded via `<link>`.

It replaced the previous React/Vite/Storybook app (`docs/landing-page/`), which
required a Node toolchain and ~50 npm dependencies to produce the same single
static `index.html`.

## How it ships

`make html` runs `merge-versions`, which copies `landing/*` into
`build/html/` so `landing/index.html` becomes the site root `index.html` and
`landing/assets/` sits alongside the versioned docs (`/2.0/`, `/1.0/`).

## Editing

Edit `index.html` directly. Asset paths are root-relative (e.g.
`assets/icons/ts.svg`), matching where they land at the deployed site root.

Features: light/dark theme toggle (follows the OS preference, with a manual
override persisted to `localStorage`), responsive 320–2560px, WCAG 2.1 AA in
both modes, dark code panels in both themes, and reduced-motion support.

## Open Graph image

`assets/og-cover.png` (1200×630) is the link-preview image used by `og:image`
and `twitter:image`. It is rendered from `og-cover-source.html` (an Ember-themed
HTML card). To regenerate after editing that card, screenshot it at a 1200×630
viewport and overwrite `assets/og-cover.png`, e.g.:

```
# serve this dir, then capture og-cover-source.html at 1200x630 -> assets/og-cover.png
# (any headless-browser screenshot tool works; keep it PNG, exactly 1200x630)
```

`og-cover-source.html` and this README are NOT shipped to the site root; the
Makefile copies only `index.html` and `assets/`.
