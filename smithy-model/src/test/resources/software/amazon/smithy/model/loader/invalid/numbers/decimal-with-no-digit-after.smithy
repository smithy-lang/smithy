// Parse error at line 7, column 9 near `)\n`: Invalid number '1.': '.' must be followed by a digit
namespace smithy.example

@trait
integer test

@test(1.)
string MyString
