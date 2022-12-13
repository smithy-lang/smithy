$version: "2.0"

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
    output: SayHelloOutput
}

@input
structure SayHelloInput {
    foo: String,
}

@output
structure SayHelloOutput {}
