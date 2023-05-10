// Syntax error at line 7, column 7: Invalid number '1.0e+': 'e', '+', and '-' must be followed by a digit | Model
namespace smithy.example

@trait
integer test

@test(1.0e+)
string MyString
