$version: "2.1"

namespace smithy.example

resource MixedResource with [MixinResource] {
}

@internal
@mixin
resource MixinResource {
}
