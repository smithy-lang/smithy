$version: "1.0"

namespace smithy.example

operation GetFoo {
    input: GetFooRequest,
    output: GetFooResponse
}

@input
structure GetFooRequest {}

@output
structure GetFooResponse {}
