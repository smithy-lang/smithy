namespace smithy.example

@documentation("""
    """)
string EmptyString

@documentation("""

    """)
string NewlineString

@documentation("""
    Hello
    """)
string TrailingNewlineString

@documentation("""
    Hello""")
string NoTrailingNewlineString

@documentation("""
    Hi \\
    there \\
    bye""")
string EscapedNewlineString

@documentation("""
    Hi \uE05A.
    """)
string WithUnicode

// If the last line is offset to the right, it's discarded since it's all whitespace.
@documentation("""
    Hello
    There
    Ok
         """)
string DiscardLastLineOffset

// Empty lines and lines with only ws do not contribute to incidental ws.
@documentation("""
    Hello

    there
    """)
string EmptyLinesAreIncidentalWs

@documentation("""
    f""")
string SingleCharacterTextBlock

@documentation("""
    \"Hi\"""")
string EmbeddedQuotesTextBlock

