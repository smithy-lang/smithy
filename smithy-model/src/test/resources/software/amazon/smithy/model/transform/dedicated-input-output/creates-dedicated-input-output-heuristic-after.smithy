$version: "2.0"

namespace smithy.example

operation GetFoo {
    input: GetFooRequest,
    output: GetFooResponse
}

@input
structure GetFooRequest {}

@output
structure GetFooResponse {}
