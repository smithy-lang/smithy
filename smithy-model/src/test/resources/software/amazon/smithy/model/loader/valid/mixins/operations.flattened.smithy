$version: "2.0"

namespace smithy.example

@error("client")
structure ConcreteError {}

@error("server")
structure MixinError {}

@internal
operation ConcreteOperation {
    errors: [MixinError, ConcreteError]
}
