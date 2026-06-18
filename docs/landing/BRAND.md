# Smithy Brand & Design System

The visual language for the Smithy landing page (and a reference for propagating
it to the rest of smithy.io). This is the **"Ember"** system: warm, crafted, and
forged around Smithy's anvil mark and a single confident red.

It is the canonical source for the design tokens that live in
`landing/index.html`. When you change a token here, change it there (and vice
versa). The page defines all color in two CSS blocks — `html[data-mode="dark"]`
and `html[data-mode="light"]` — and everything else references `var(--token)`.

---

## Brand personality

Precise, engineering-grade, trustworthy. Smithy is an open-source IDL created
and used at AWS; the page should feel like infrastructure you can rely on, not
a startup splash. The anvil/forge heritage (the name, the mark) is the one
ownable, characterful thread — used sparingly, never kitsch.

Voice: clear, declarative, technical, confident, not hypey. Matches the Smithy
2.0 docs. "type-safe", "backward compatibility". No invented metrics, logos, or
testimonials. Truthful claims only (open source, Apache-2.0, used at AWS,
multi-language).

---

## Logo & mark

| Asset | Use |
|---|---|
| `assets/smithy-anvil.svg` | The red anvil mark. Favicon, footer, decorative nodes. |
| `assets/smithy-dark.svg` | Full wordmark with **light** text — use on **dark** backgrounds. |
| `assets/smithy.svg` | Full wordmark with **dark** text — use on **light** backgrounds. |

The wordmark already contains an anvil; never place a standalone anvil next to
the wordmark (no double mark).

---

## Color

Red is the single saturated accent. Everything else is warm neutral. Code panels
are **always dark**, in both light and dark modes.

### Dark mode (default)

| Token | Value | Role |
|---|---|---|
| `--bg` | `#14110f` | Page background (warm charcoal) |
| `--surface` | `#1d1916` | Cards, nav, panels |
| `--surface-2` | `#262019` | Deeper insets |
| `--border` | `rgba(255,255,255,.08)` | Hairlines / dividers |
| `--text` | `#f4efe9` | Primary text |
| `--muted` | `#a89f95` | Secondary text |
| `--accent` | `#df231d` | Smithy red — fills, borders, icons (non-text) |
| `--accent-bright` | `#ff4339` | Brighter accent for hover / large text |
| `--accent-hover` | `#cf1e18` | Solid-button hover fill (keeps white label AA) |
| `--accent-text` | `#ff5a51` | Accent used as **normal-size text** (AA on `--bg`) |
| `--glow` | `rgba(223,35,29,.22)` | The single ambient hero glow |
| `--ring` | `#ff6f66` | Focus ring |

### Light mode

| Token | Value | Role |
|---|---|---|
| `--bg` | `#faf7f2` | Page background (warm parchment / "beige") |
| `--surface` | `#ffffff` | Cards, nav, panels |
| `--surface-2` | `#f3ede4` | Deeper insets |
| `--border` | `#e7e0d6` | Hairlines / dividers |
| `--text` | `#1a1714` | Primary text |
| `--muted` | `#6b6258` | Secondary text |
| `--accent` | `#c41c16` | Red, deepened for AA on light |
| `--accent-bright` / `--accent-hover` | `#b01810` / `#b8170f` | Hover (darkens on light) |
| `--accent-text` | `#c41c16` | Accent text (5.58:1 on `--bg`) |
| `--glow` | `rgba(207,31,25,.16)` | Ambient glow |
| `--ring` | `#c41c16` | Focus ring |

### Code panels (dark in BOTH modes — mode-stable)

| Token | Value |
|---|---|
| `--code-bg` | `#0f0c0b` |
| `--code-surface` | `#181311` |
| `--code-text` | `#ece5dd` |
| `--code-muted` | `#8a7f74` |
| `--tok-key` | `#d9a48c` (keywords) |
| `--tok-trait` | `#ff4339` (`@traits` — the Smithy signature; keep red) |
| `--tok-type` | `#e7c98a` (types) |
| `--tok-str` | `#9fd29a` (strings) |
| `--tok-num` | `#e0a36a` (numbers) |
| `--code-accent` | `#ff5a51` (in-panel prompts / tags) |

Rule: inside code panels use only `--code-*` / `--tok-*` / `--code-accent`.
Never the page accent tokens — they flip per mode and fail AA on the dark panel
in light mode.

### Usage rules
- One saturated red. Buttons are solid fills (no gradient buttons); links are
  solid color (no gradient text). Background glows are subtle and atmospheric.
- No pink / magenta / fuchsia. Red sits in the `#c41c16`–`#ff5a51` range only.
- `@traits` in code are the focal red moment.

---

## Typography

| Token | Family | Use |
|---|---|---|
| `--font-display` | **Schibsted Grotesk** | Headings, UI, body |
| `--font-mono` | **JetBrains Mono** | Code, terminal, eyebrows, labels, metadata |

Loaded from Google Fonts. Display weights 400–800; mono 400–700. Headlines use
800 with tight tracking (~`-.03em`); body is 400–500. Monospace uppercase with
wide letter-spacing (`.08–.18em`) is the "spec label" motif (eyebrows, group
labels, captions).

---

## Layout & motion tokens

| Token | Value | Role |
|---|---|---|
| `--maxw` | `1180px` (`1320px` ≥1800px) | Content width |
| `--gutter` | `clamp(20px, 5vw, 72px)` | Side padding |
| `--radius` / `--radius-sm` | `14px` / `9px` | Corner radii |
| `--ease` | `cubic-bezier(.22,.61,.36,1)` | Standard easing |

Motion: one orchestrated page-load reveal; subtle hover lifts (`translateY`) +
an `↗` cue on external link cards. All motion collapses under
`prefers-reduced-motion`.

---

## Accessibility (non-negotiable)

WCAG 2.1 AA in both modes. Skip link, visible `:focus-visible` rings, semantic
landmarks, single `<h1>` + ordered headings, real `<button>` theme toggle with
`aria-pressed`, mobile hamburger with `aria-expanded`/Esc/focus management,
≥44px tap targets, readable at 200% zoom. Mobile-friendly 320–2560px with no
horizontal overflow. Every contrast pair above meets AA — verify any new pair
before shipping.

---

## Theming the rest of smithy.io (open question from review)

The docs currently use Furo (Sphinx) with its own palette. To carry Ember across
the site:

- **Light "beige" surface:** map Furo's `--color-background-primary` to `--bg`
  `#faf7f2` and content text to `#1a1714`; brand/accent to `#c41c16`. This
  propagates the parchment look Hector liked.
- **Dark mode:** Furo's default dark is cool-gray; retune toward the Ember
  charcoal (`#14110f` bg, `#1d1916` surfaces, `#f4efe9` text, `#ff5a51` links)
  so the docs match the landing page. This is the "dark mode may need tweaking"
  item — the landing tokens above are the target.
- **Code blocks:** the docs already use a dark Pygments theme; align it to the
  `--code-*` / `--tok-*` values so syntax highlighting matches the landing page.
- This is a **fast-follow**, not part of the landing-page change. Blog / "what's
  new" areas would start as nav links up top and can become sections later.

---

This doc is the artifact to update first when the brand evolves; the page tokens
follow it.
