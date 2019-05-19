# -*- coding: utf-8 -*-
"""
Smithy IDL lexer
~~~~~~~~~~~~~~~~

Lexers for the Smithy IDL.
"""

import re

from pygments.lexer import RegexLexer, default, include, bygroups
from pygments.token import Text, Comment, Keyword, Name
from pygments.token import Literal, String, Operator

__all__ = ['SmithyLexer']


class SmithyLexer(RegexLexer):
    """
    For Smithy IDL
    """
    name = 'Smithy'
    filenames = ['*.smithy']
    aliases = ['smithy', 'Smithy']

    flags = re.MULTILINE | re.UNICODE
    unquoted = r'[A-Z-a-z0-9_\.#$-]+'
    identifier = r"[A-Z-a-z0-9_\.#$-]+"

    tokens = {
        'root': [
            (r'//.*$', Comment),
            (r'@[0-9a-zA-Z\.#-]*', Name.Decorator),
            (r'(->|=)', Name.Decorator),
            (r'errors', Name.Decorator),
            (r'^(\$version)(:)(.+)', bygroups(Keyword.Declaration, Name.Decorator, Name.Class)),
            (r'^(namespace)(\s+' + identifier + r')\b', bygroups(Keyword.Declaration, Name.Class)),
            (r'^(byte|short|integer|long|float|'
             r'double|bigInteger|bigDecimal|boolean|blob|string|timestamp)(\s+' + identifier + r')\b',
             bygroups(Keyword.Declaration, Name.Class)),
            (r'^(apply|list|map|set|structure|union|resource|operation|service|trait)(\s+' + identifier + r')',
             bygroups(Keyword.Declaration, Name.Class)),
            (r'^(metadata)(\s+.+)\s*(=)',
             bygroups(Keyword.Declaration, Name.Class, Name.Decorator)),
            (identifier + ":", Name.Label),
            (identifier, Name),
            (r'\[', Text, "#push"),
            (r'\]', Text, "#pop"),
            (r'\(', Text, "#push"),
            (r'\)', Text, "#pop"),
            (r'{', Text, "#push"),
            (r'}', Text, "#pop"),
            (r'"{3}(\\\\|\n|\\")*"{3}', String.Double),
            (r'"(\\\\|\n|\\"|[^"])*"', String.Double),
            (r"'(\\\\|\n|\\'|[^'])*'", String.Single),
            (r'[:,\s]+', Text),
        ]
    }
