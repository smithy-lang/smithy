namespace smithy.example

resource Foo {
    identifier: {id: FooId},
    read: GetFoo,
}

string FooId

@readonly
operation GetFoo(GetFooInput) -> GetFooOutput

structure GetFooInput {
    @required
    id: FooId,
}

structure GetFooOutput {}
