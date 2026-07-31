# -*- coding: utf-8 -*-
"""
Ember Pygments style
~~~~~~~~~~~~~~~~~~~~~

A syntax highlighting style that matches the smithy.io landing page's code
panels (the "Ember" design system, see docs/landing/BRAND.md). Warm tokens on a
near-black panel, with the Smithy ``@trait`` (Name.Tag) as the one saturated
red — the signature accent. Token colors mirror the landing page's ``--tok-*``
custom properties so the docs and homepage read as one site.
"""

from pygments.style import Style
from pygments.token import (
    Comment,
    Error,
    Generic,
    Keyword,
    Name,
    Number,
    Operator,
    Punctuation,
    String,
    Text,
)

# Palette — mirrors docs/landing/index.html (Ember code panel).
_BG = "#19120d"        # code panel background — rgb(25, 18, 13)
_TEXT = "#ece5dd"      # --code-text: default code foreground
_MUTED = "#8a7f74"     # --code-muted: comments / secondary
_KEY = "#d9a48c"       # --tok-key: keywords ($version, service, resource, ...)
_TRAIT = "#ff4339"     # --tok-trait: @traits — the one saturated red
_TYPE = "#e7c98a"      # --tok-type: types / shape identifiers
_STR = "#9fd29a"       # --tok-str: strings
_PUNC = "#9b9088"      # --tok-punc: punctuation / braces
_NUM = "#e0a36a"       # --tok-num: numbers
# Member names (left of the ":") — a warm light tan, distinct from both the
# bright default text and the dim comment color.
_MEMBER = "#ccbdac"
# Comments sit slightly dimmer than the muted member color.
_COMMENT = "#6f655c"


class EmberStyle(Style):
    """Warm, near-black syntax theme matching the Smithy landing page."""

    name = "ember"
    background_color = _BG
    highlight_color = "#2a2320"
    line_number_color = "#6f655c"
    line_number_background_color = _BG

    styles = {
        Text: _TEXT,
        Error: _TRAIT,

        Comment: f"italic {_COMMENT}",
        Comment.Preproc: _COMMENT,

        # Smithy: ``///`` documentation comments are String.Doc.
        String.Doc: f"italic {_COMMENT}",
        String: _STR,
        String.Escape: _NUM,

        # Keywords: shape/declaration keywords (service, resource, structure,
        # operation...) render bold. Control statements ($version) and
        # constants are keywords too; the lexer tags these Keyword.* / Name.Label.
        Keyword: f"bold {_KEY}",
        Keyword.Constant: _KEY,
        Keyword.Declaration: f"bold {_KEY}",
        Name.Label: _KEY,

        # @traits are the signature red — the focal token.
        Name.Tag: _TRAIT,
        Name.Decorator: _TRAIT,
        Name.Attribute: _TRAIT,

        # Types / shape identifiers.
        Name.Class: _TYPE,
        Name.Namespace: _TYPE,
        Name.Variable.Class: _TYPE,
        Name.Function: _TYPE,
        Name.Builtin: _TYPE,

        # Member names render dim, but a touch warmer/lighter than comments.
        Name.Property: _MEMBER,

        Number: _NUM,

        Operator: _PUNC,
        Punctuation: _PUNC,

        # Generic tokens (diffs, tracebacks, prompts in non-Smithy blocks).
        Generic.Heading: f"bold {_TEXT}",
        Generic.Subheading: f"bold {_TYPE}",
        Generic.Deleted: _TRAIT,
        Generic.Inserted: _STR,
        Generic.Emph: "italic",
        Generic.Strong: "bold",
        Generic.Prompt: _MUTED,
        Generic.Output: _TEXT,
        Generic.Error: _TRAIT,
    }
