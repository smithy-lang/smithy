# -*- coding: utf-8 -*-
"""
Smithy IDL lexer
~~~~~~~~~~~~~~~~

Lexers for the Smithy IDL.
"""

import re

from pygments.lexer import RegexLexer, bygroups, words, include
from pygments.token import (
    Text,
    Comment,
    Keyword,
    Name,
    String,
    Number,
    Whitespace,
    Punctuation,
)

__all__ = ["SmithyLexer"]


class SmithyLexer(RegexLexer):
    name = "Smithy"
    url = "https://smithy.io/"
    filenames = ["*.smithy"]
    aliases = ["smithy"]

    flags = re.MULTILINE | re.UNICODE
    shape_id = r"[A-Za-z0-9_\.#$-]+"
    identifier = r"[a-zA-Z_]\w*"

    tokens = {
        "root": [
            # Regular comment
            (r"//.*$", Comment),
            # Triple-slash indicates a documentation comment
            (r"///.*$", String.Doc),
            # Traits
            (r"@[0-9a-zA-Z\.#-]*", Name.Tag),
            # Control statements
            (
                r"^(\$" + identifier + r")(:)",
                bygroups(Keyword.Declaration, Name.Decorator),
            ),
            # Namespace statement
            (
                r"^(namespace\s+)(" + shape_id + r")\b",
                bygroups(Keyword.Declaration, Name.Class),
            ),
            # Metadata statements
            (
                r"^(metadata\s+)(.+)(\s*)(=)",
                bygroups(Keyword.Declaration, Name.Class, Whitespace, Name.Decorator),
            ),
            include("keywords"),
            include("constants"),
            (r"\$" + identifier, Name.Label),
            (
                "(" + identifier + r")(\s*)(:=?)",
                bygroups(Name.Property, Whitespace, Punctuation),
            ),
            (shape_id, Name.Variable.Class),
            (r"(?:\[|\(|\{)", Text, "#push"),
            (r"(?:\]|\)|\})", Text, "#pop"),
            (r'"""', String.Double, "textblock"),
            (r'"', String.Double, "string"),
            (r"[:,]+", Punctuation),
            (r"\s+", Whitespace),
            (r"=", Punctuation)
        ],
        "textblock": [
            (r"\\(.|$)", String.Escape),
            (r'"""', String.Double, "#pop"),
            (r'[^\\"]+', String.Double),
            (r'"', String.Double),
        ],
        "string": [
            (r"\\.", String.Escape),
            (r'"', String.Double, "#pop"),
            (r'[^\\"]+', String.Double),
        ],
        "keywords": [
            (
                # These are all keywords that must be followed by an identifier
                # of some sort. They're mostly, but not entirely, shapes.
                words(
                    (
                        (
                            "use ",
                            "apply ",
                            "byte ",
                            "short ",
                            "integer ",
                            "intEnum ",
                            "long ",
                            "float ",
                            "document ",
                            "double ",
                            "bigInteger ",
                            "bigDecimal ",
                            "boolean ",
                            "blob ",
                            "string ",
                            "enum ",
                            "timestamp ",
                            "list ",
                            "map ",
                            "set ",
                            "structure ",
                            "union ",
                            "resource ",
                            "operation ",
                            "service ",
                        )
                    ),
                    prefix=r"^",
                    suffix=r"(\s*" + shape_id + r")\b",
                ),
                bygroups(Keyword.Declaration, Name.Class),
            ),
            (words(("with", "for"), suffix=r"\b"), Keyword),
        ],
        "constants": [
            (words(("true", "false", "null"), suffix=r"\b"), Keyword.Constant),
            (r"(-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?)", Number),
        ],
    }
