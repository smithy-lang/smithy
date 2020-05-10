// Parse error at line 7, column 12 near `)\n`: Invalid number '1.0e+': 'e', '+', and '-' must be followed by a digit
namespace smithy.example

@trait
integer test

@test(1.0e+)
string MyString
