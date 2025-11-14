// Syntax error at line 7, column 7: Expected one of LBRACE('{'), LBRACKET('['), TEXT_BLOCK('"""'), BYTE_TEXT_BLOCK('b"""'), STRING('"'), BYTE_STRING('b"'), NUMBER, IDENTIFIER; but found DOT('.') | Model
namespace smithy.example

@trait
integer test

@test(.1)
string MyString
