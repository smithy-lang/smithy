$version: "2.0"

namespace smithy.example

operation GetFoo {
    input: GetFooOperationInput,
    output: GetFooOperationOutput
}

@input
structure GetFooOperationInput {
    @required
    id: String
}

@output
structure GetFooOperationOutput {
    @required
    id: String,

    @required
    createdAt: Timestamp
}

structure GetFooData {
    @required
    id: String
}

structure Foo {
    @required
    id: String,

    @required
    createdAt: Timestamp
}

structure Reused {
    foo: Foo,
    data: GetFooData
}

structure GetFooInput {}

structure GetFooOutput {}
