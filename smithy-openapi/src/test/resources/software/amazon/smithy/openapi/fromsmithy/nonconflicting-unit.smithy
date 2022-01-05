$version: "1.0"

namespace example.rest

use aws.protocols#restJson1

@restJson1
service RestService {
    version: "1",
    operations: [Ping, Ping2]
}

@http(method: "POST", uri: "/ping")
operation Ping {
    // Implicit input: Unit,
    output: PingOutput
}

@http(method: "POST", uri: "/ping2")
operation Ping2 {
    input: Ping2Input
    // Implicit output: Unit
}

@output
structure PingOutput {
    unit: Unit
}

@input
structure Ping2Input {
    unit: Unit
}

// This shape does not conflict with the implicit Unit shape.
structure Unit {}
