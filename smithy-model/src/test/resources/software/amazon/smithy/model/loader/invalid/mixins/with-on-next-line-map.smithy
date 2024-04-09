// Syntax error at line 7, column 5: Expected LBRACE('{') but found IDENTIFIER('with') | Model
$version: "2.0"

namespace smithy.example

map MixedMap
    with [MixinMap] {}

@mixin
map MixinMap {
    key: String
    value: String
}
