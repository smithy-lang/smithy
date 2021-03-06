namespace smithy.example

service MyService {
    version: "1",
    operations: [
        SayHello,
    ],
    rename: {
        "smithy.example#Foo": "A",
        "smithy.example#Baz": "A",
    }
}

operation SayHello {
    input: SayHelloInput,
}

structure SayHelloInput {
    foo: Foo,
    baz: Baz,
}

structure Foo {}

structure Baz {}
