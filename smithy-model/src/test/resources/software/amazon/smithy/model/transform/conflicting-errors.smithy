$version: "2.0"

namespace smithy.example

service MyService {
    operations: [MyOperation]
    errors: [FooServiceLevelError, BarServiceLevelError]
}

operation MyOperation {
    input := {
        string: String
    }
    errors: [FooError, BarError]
}

@error("client")
@httpError(429)
structure FooServiceLevelError {
    @httpHeader("x-service-foo")
    xServiceFoo: String

    @httpHeader("x-common")
    xCommon: String
}

@error("client")
@httpError(429)
structure BarServiceLevelError {
    @httpHeader("x-service-bar")
    xServiceBar: String

    @httpHeader("x-common")
    xCommon: String
}

@error("client")
@httpError(429)
structure FooError {
    @httpHeader("x-foo")
    xFoo: String

    @httpHeader("x-common")
    xCommon: String
}

@error("client")
@httpError(429)
structure BarError {
    @httpHeader("x-bar")
    xBar: String

    @httpHeader("x-common")
    xCommon: String
}
