$version: "2.0"

namespace smithy.example

@aws.protocols#restJson1
service HasMixin {
    version: "2021-08-12",
    operations: [Greeting]
}

@http(method: "GET", uri: "/")
@readonly
operation Greeting {
    output: Output
}

@mixin
structure Mixin {
    greeting: String
}

structure Output with [Mixin] {
    language: String
}
