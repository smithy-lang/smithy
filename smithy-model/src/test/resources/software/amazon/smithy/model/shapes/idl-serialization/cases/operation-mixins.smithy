$version: "2.1"

namespace smithy.example

operation ConcreteOperation with [InternalMixin] {
    input: Unit
    output: Unit
    errors: [
        ConcreteError
    ]
}

@internal
@mixin
operation InternalMixin {
    input: Unit
    output: Unit
    errors: [
        MixinError
    ]
}

@error("client")
structure ConcreteError {}

@error("server")
structure MixinError {}
