namespace smithy.example

@documentation(b"""
    """)
string EmptyString

@documentation(b"""

    """)
string NewlineString

@documentation(b"""
    Hello
    """)
string TrailingNewlineString

@documentation(b"""
    Hello""")
string NoTrailingNewlineString

@documentation(b"""
    Hi \
    there \
    bye""")
string EscapedNewlineString

@documentation(b"""
    Hi \uE05A.
    """)
string WithUnicode

@documentation(b"""
    Hi\x20there
    """)
string WithHex

@documentation(b"""
    Hi\0
    """)
string WithNullByte

// If the last line is offset to the right, it's discarded since it's all whitespace.
@documentation(b"""
    Hello
    There
    Ok
         """)
string DiscardLastLineOffset

// Empty lines and lines with only ws do not contribute to incidental ws.
@documentation(b"""
    Hello

    there
    """)
string EmptyLinesAreIncidentalWs

@documentation(b"""
    f""")
string SingleCharacterTextBlock

@documentation(b"""
    "Hi\"""")
string EmbeddedQuotesTextBlock
