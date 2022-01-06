$version: "2.0"

namespace smithy.example

@error("client")
structure ConcreteError {}

@error("server")
structure MixinError {}

@mixin
@internal
operation InternalMixin {
    errors: [MixinError]
}

operation ConcreteOperation with [InternalMixin] {
    errors: [ConcreteError]
}
