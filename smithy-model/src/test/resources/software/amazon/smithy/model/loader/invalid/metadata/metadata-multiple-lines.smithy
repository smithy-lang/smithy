// Syntax error at line 3, column 16: Expected one of STRING('"'), BYTE_STRING('b"'), TEXT_BLOCK('"""'), BYTE_TEXT_BLOCK('b"""'), NUMBER, IDENTIFIER, LBRACE('{'), LBRACKET('['); but found COMMENT('// this is not allowed\n') | Model
$version: "2.0"
metadata foo = // this is not allowed
"bar"
