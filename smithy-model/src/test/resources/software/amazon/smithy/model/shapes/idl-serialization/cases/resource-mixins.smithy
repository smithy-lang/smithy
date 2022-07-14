$version: "2.0"

namespace smithy.example

resource MixedResource with [MixinResource] {
}

@internal
@mixin
resource MixinResource {
}
