$version: "1.0"

namespace smithy.example

@aws.protocols#restJson1
service MyService {
    version: "2020-07-02",
    operations: [GetSomething],
    shapes: [OtherStructure],
}

@http(method: "GET", uri: "/")
operation GetSomething {
    output: GetSomethingOutput,
}

structure GetSomethingOutput {}

structure OtherStructure {}
