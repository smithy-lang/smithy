// Syntax error at line 3, column 16: Expected one of STRING('"'), TEXT_BLOCK('"""'), NUMBER, IDENTIFIER, LBRACE('{'), LBRACKET('['); but found COMMENT('// this is not allowed\n') | Model
$version: "2.0"
metadata foo = // this is not allowed
"bar"
