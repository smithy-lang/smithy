// Expected one of LBRACE('{'), LBRACKET('['), TEXT_BLOCK('"""'), STRING('"'), NUMBER, IDENTIFIER; but found POUND('#')
$version: "2"

namespace smithy.example

@pattern(#re "^\d+$")
string Foo
