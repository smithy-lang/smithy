$version: "2"

namespace smithy.example

resource MixedResource with [MixinResource] {
}

@internal
@mixin
resource MixinResource {
}
