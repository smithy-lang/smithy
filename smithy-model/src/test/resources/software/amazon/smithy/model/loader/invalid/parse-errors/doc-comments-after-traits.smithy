// Syntax error at line 7, column 1: Expected IDENTIFIER but found DOC_COMMENT('/// This is invalid.\n')
$version: "2.0"

namespace com.foo

@deprecated
/// This is invalid.
string MyString
