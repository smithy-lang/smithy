$version: "2.0"

namespace smithy.example

operation GetFoo {
    input: GetFooInput,
    output: GetFooOutput
}

@input
structure GetFooInput {
    @required
    id: String
}

@output
structure GetFooOutput {
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
