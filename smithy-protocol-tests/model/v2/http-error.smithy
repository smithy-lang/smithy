$version: "2.0"

namespace smithy.protocoltests.corpus

// HttpErrorProtocolTestServices exercises @httpError, which overrides the
// status code that @error("client"|"server") would otherwise imply, in its own
// layer because some protocol families support @httpError without supporting
// the HTTP binding traits.
@mixin
service HttpErrorProtocolTestService {
    operations: [
        HttpErrorOperation
    ]
}

operation HttpErrorOperation {
    input: HttpErrorOperationInput
    output: HttpErrorOperationOutput
    errors: [
        HttpErrorConflict
        HttpErrorGone
        HttpErrorServiceUnavailable
    ]
}

structure HttpErrorOperationInput {}

structure HttpErrorOperationOutput {}

@error("client")
@httpError(409)
structure HttpErrorConflict {
    message: String
}

@error("client")
@httpError(410)
structure HttpErrorGone {
    message: String
    details: String
}

@error("server")
@httpError(503)
structure HttpErrorServiceUnavailable {
    message: String
    retryAfterSeconds: Integer
}
