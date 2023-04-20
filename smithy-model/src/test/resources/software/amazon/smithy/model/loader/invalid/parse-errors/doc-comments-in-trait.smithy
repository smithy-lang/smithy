// Syntax error at line 7, column 5: Expected one of STRING('"'), TEXT_BLOCK('"""'), NUMBER, IDENTIFIER, LBRACE('{'), LBRACKET('['); but found DOC_COMMENT('/// Invalid!\n')
$version: "2.0"
namespace smithy.example

/// Hello
@enum([
    /// Invalid!
    { name: "X", value: "X"}
]) string Features

@documentation("X")
map Foo {
key: Features,
value: Boolean
}
