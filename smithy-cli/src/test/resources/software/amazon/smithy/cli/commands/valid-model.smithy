$version: "2.0"

namespace smithy.example

resource Foo {
    identifiers: {id: FooId},
    read: GetFoo,
}

string FooId

@readonly
operation GetFoo {
    input: GetFooInput,
    output: GetFooOutput
}

structure GetFooInput {
    @required
    id: FooId,
}

structure GetFooOutput {}
