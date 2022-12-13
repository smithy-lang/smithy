$version: "2.0"

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
    output: SayHelloOutput
}

@input
structure SayHelloInput {}

@output
structure SayHelloOutput {}
