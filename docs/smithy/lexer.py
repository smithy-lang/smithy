# -*- coding: utf-8 -*-
"""
Smithy IDL lexer
~~~~~~~~~~~~~~~~

Lexers for the Smithy IDL.
"""

from pygments.lexer import inherit
from pygments.lexers import SmithyLexer as _SmithyLexer
from pygments.token import String

__all__ = ["SmithyLexer"]


class SmithyLexer(_SmithyLexer):
    # The upstream lexer doesn't properly handle text blocks
    tokens = {"root": [(r'"""(?:.|\n)*?[^\\]"""', String.Single), inherit]}
