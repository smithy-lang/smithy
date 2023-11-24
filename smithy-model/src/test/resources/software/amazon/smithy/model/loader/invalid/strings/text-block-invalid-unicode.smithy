// Syntax error at line 4, column 16: Error parsing text block: Invalid unclosed unicode escape found in string | Model
namespace smithy.example

@documentation("""
    \ua""")
string MyString
