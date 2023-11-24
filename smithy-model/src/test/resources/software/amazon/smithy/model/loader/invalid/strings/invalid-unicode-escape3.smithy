// Syntax error at line 4, column 16: Error parsing quoted string: Invalid unclosed unicode escape found in string | Model
namespace smithy.example

@documentation("\uaaa")
string MyString
