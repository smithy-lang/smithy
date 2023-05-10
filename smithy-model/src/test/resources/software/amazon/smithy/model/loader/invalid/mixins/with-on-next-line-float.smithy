// Syntax error at line 10, column 5: Unknown shape type: with | Model
$version: "2.0"

namespace smithy.example

@mixin
float MixinFloat

float MixedFloat
    with [MixinFloat]
