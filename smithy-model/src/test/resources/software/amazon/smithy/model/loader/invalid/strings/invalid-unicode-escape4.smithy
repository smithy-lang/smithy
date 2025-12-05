// Syntax error at line 4, column 16: Error parsing quoted string: Invalid hex character: `t` | Model
namespace smithy.example

@documentation("\uaaat")
string MyString
