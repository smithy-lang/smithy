// Syntax error at line 7, column 5: Expected LBRACE('{') but found IDENTIFIER('with') | Model
$version: "2.0"

namespace smithy.example

union MixedUnion
    with [MixinUnion] {}

@mixin
union MixinUnion {}
