$version: "2.0"

metadata suppressions = [
    {
        id: "UnstableTrait.smithy.api#discriminated"
        namespace: "*"
        reason: "The trait is unstable, which is not what this file tests."
    }
]

namespace smithy.example

/// Invalid: a streaming union is an event stream, which does not use the discriminated encoding. This is in its own
/// file because a trait conflict stops other validators from reporting events.
@streaming
@discriminated("type")
union Streamed {
    circle: Circle
}

structure Circle {
    radius: Integer
}
