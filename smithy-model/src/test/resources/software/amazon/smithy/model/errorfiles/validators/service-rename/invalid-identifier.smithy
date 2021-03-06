namespace smithy.example

service MyService {
    version: "1",
    operations: [
        SayHello,
    ],
    rename: {
        "smithy.example#Foo": "no!"
    }
}

operation SayHello {
    input: SayHelloInput,
}

structure SayHelloInput {
    foo: Foo
}

structure Foo {}
