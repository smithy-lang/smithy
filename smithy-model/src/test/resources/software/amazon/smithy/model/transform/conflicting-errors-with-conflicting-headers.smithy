$version: "2.0"

namespace smithy.example

service MyService {
    operations: [MyOperation]
}

operation MyOperation {
    input := {
        string: String
    }
    errors: [FooError, BarError]
}

@error("client")
@httpError(429)
structure FooError {
    @httpHeader("x-foo")
    xFoo: String
}

@error("client")
@httpError(429)
structure BarError {
    @httpHeader("x-bar")
    xFoo: String
}
