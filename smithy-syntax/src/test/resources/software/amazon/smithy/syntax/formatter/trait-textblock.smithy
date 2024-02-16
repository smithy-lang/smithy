$version: "2.0"

namespace smithy.example

// This will look exactly the same when formatted.
@documentation(
    """
    This is the documentation for Foo.
    Lorem ipsum dolor."""
)
string Foo

// This will look exactly the same when formatted.
@documentation(
    """
    This is the documentation for Bar.
    Lorem ipsum dolor.
    """
)
string Bar

// The quotes and text will be indented inside the parens when formatted.
@documentation(
"""
This is the documentation for Baz.
Lorem ipsum dolor.
"""
)
string Baz

// The opening quotes will be indented and move to the next line, the contents of the block will remain indented
// because indentation is based on the closing quote, the closing quote will be indented, and the closing paren on
// the next line.
@documentation("""
    This is the documentation for Bux.
    Lorem ipsum dolor.
""")
string Bux

// The opening quote will move to the next line, indentend, the content will remain the same, the closing quote
// will stay on last line, and the closing paren will move to the next line.
@documentation("""
    This doc string must not have the leading whitespace altered.

    {
        "foo": true,
        bar: [
            false
        ]
    }""")
string JsonDocsNoTrailingNewLine

// The opening quote will move to the next line, indentend, the content will remain the same, the closing quote
// will stay on its own line, and the closing paren will move to the next line.
@documentation("""
    This doc string must not have the leading whitespace altered.

    {
        "foo": true,
        bar: [
            false
        ]
    }
    """)
string JsonDocsTrailingNewLine

// Ensure extra newlines at the end are included.
@documentation("""
    Hello with extra newlines.


    """)
string ExtraTrailingNewlines

// Moves the quotes to the same indentation level and the closing parens to the next line.
@documentation("""
    """)
string EmptyTextBlock
