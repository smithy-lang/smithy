// Syntax error at line 4, column 16: Error parsing quoted string: Invalid escape found in string: `\x` | Model
namespace smithy.example

@documentation("\x00")
string MyString
