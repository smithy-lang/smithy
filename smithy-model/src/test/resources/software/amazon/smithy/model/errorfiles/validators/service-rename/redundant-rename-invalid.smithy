namespace smithy.example

service MyService {
    version: "1",
    operations: [
        SayHello,
    ],
    rename: {
        "smithy.example#SayHelloInput": "SayHelloInput"
    }
}

operation SayHello {
    input: SayHelloInput,
}

structure SayHelloInput {}
