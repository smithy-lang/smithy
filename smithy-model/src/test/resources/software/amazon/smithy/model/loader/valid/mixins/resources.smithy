$version: "2.0"

namespace smithy.example

@mixin
@internal
resource MixinResource {}

resource MixedResource with [MixinResource] {}
