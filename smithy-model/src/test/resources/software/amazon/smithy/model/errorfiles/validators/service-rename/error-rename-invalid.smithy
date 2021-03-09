namespace smithy.example

service MyService {
    version: "1",
    operations: [
        SayHello,
    ],
    rename: {
        "smithy.example#BadGreeting": "ThisDoesNotWork"
    }
}

operation SayHello {
    errors: [BadGreeting]
}

@error("client")
structure BadGreeting {}
