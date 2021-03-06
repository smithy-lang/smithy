namespace smithy.example

service MyService {
    version: "1",
    operations: [
        SayHello,
    ],
    rename: {
        "smithy.example#SayHelloInput$foo": "thisDoesNotWork"
    }
}

operation SayHello {
    input: SayHelloInput,
}

structure SayHelloInput {
    foo: String,
}
