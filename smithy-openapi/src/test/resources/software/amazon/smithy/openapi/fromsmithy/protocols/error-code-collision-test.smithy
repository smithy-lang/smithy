namespace example

use aws.protocols#restJson1
use smithy.framework#ValidationException

@restJson1
service Example {
    version: "2006-03-01",
    operations: [GetCurrentTime],
    errors: [
        Error1,
        Error2,
        Error3,
        Error4,
        Error5,
        Error6,
        Error7,
        Error8
    ]
}

@error("client")
@retryable(throttling: true)
@httpError(429) // Too many requests
structure Error1 {
    @httpHeader("error1-header")
    @required
    header: String,

    @required
    message: String,
}

@error("client")
@retryable(throttling: true)
@httpError(429) // Too many requests
structure Error2 {
    @httpHeader("error2-header")
    @required
    header2: String,

    @required
    message: String,

    message2: String,

    message3: String
}

@error("client")
@retryable(throttling: true)
@httpError(429) // Too many requests
structure Error3 {
    @required
    message24 : String,
}

@error("client")
@retryable(throttling: true)
@httpError(429) // Too many requests
structure Error4 {
    @required
    message365: String,
}

@error("client")
@retryable(throttling: true)
@httpError(429) // Too many requests
structure Error5 {
    @required
    message: String,
}

@error("client")
@retryable(throttling: true)
@httpError(429) // Too many requests
structure Error6 {
    @required
    message: String,
}

@error("client")
@httpError(404) // Too many requests
structure Error7 {
    @required
    message: String,
}

@error("client")
@httpError(404) // Too many requests
structure Error8 {
    @required
    message2: String,
}


@readonly
@http(uri: "/time", method: "GET")
operation GetCurrentTime {
    input: GetCurrentTimeInput,
    output: GetCurrentTimeOutput
}

@input
structure GetCurrentTimeInput {}

@output
structure GetCurrentTimeOutput {
    @required
    time: Timestamp
}
