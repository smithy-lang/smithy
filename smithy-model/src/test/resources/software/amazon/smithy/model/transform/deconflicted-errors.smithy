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
    errors: [MyOperation429Error]
}

@error("client")
@httpError(429)
structure MyOperation429Error {
    errorUnion: MyOperation429ErrorContent
    @httpHeader("x-foo")
    xFoo: String
    @httpHeader("x-bar")
    xBar: String
    @httpHeader("x-common")
    xCommon: String
    @httpHeader("x-service-foo")
    xServiceFoo: String
    @httpHeader("x-service-bar")
    xServiceBar: String
}

union MyOperation429ErrorContent {
    FooError: FooError
    BarError: BarError
    FooServiceLevelError: FooServiceLevelError
    BarServiceLevelError: BarServiceLevelError
}

@error("client")
@httpError(429)
structure FooServiceLevelError {}

@error("client")
@httpError(429)
structure BarServiceLevelError {}

@error("client")
@httpError(429)
structure FooError {}

@error("client")
@httpError(429)
structure BarError {}
