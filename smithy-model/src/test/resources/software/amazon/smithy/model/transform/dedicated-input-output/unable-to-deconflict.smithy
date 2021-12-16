$version: "2.0"

namespace smithy.example

operation GetFoo {
    input: GetFooData,
    output: Foo
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

// This should never happen in practice, but this test will catch when
// the deconflicting function returns a still conflicting name.
structure GetFooInput {}
structure GetFooOutput {}
structure GetFooOperationInput {}
structure GetFooOperationOutput {}
