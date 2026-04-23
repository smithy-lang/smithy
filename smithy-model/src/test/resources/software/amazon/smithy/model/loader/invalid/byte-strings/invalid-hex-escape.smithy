// Syntax error at line 4, column 16: Error parsing byte string: Invalid unclosed hex escape found in string | Model
namespace smithy.example

@documentation(b"\x0")
string MyString
