namespace smithy.example

service MyService {
    version: "1",
    operations: [
        SayHello,
    ],
    rename: {
        "smithy.example#Foo": "Baz",
        "smithy.example#Baz": "Renamed",
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
