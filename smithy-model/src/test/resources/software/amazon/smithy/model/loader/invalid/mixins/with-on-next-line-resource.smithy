// Parse error at line 7, column 5 near `with`: Expected: '{', but found 'w'
$version: "2.0"

namespace smithy.example

resource MixedResource
    with [MixinResource] {}

@mixin
resource MixinResource {}
