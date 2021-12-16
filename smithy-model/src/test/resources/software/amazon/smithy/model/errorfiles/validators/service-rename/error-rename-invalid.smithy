$version: "2.0"

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
    input: SayHelloInput,
    output: SayHelloOutput,
    errors: [BadGreeting]
}

@input
structure SayHelloInput {}

@output
structure SayHelloOutput {}

@error("client")
structure BadGreeting {}
