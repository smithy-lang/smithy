$version: "2.0"

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
    output: SayHelloOutput
}

@input
structure SayHelloInput {
    foo: Foo
}

@output
structure SayHelloOutput {}

structure Foo {}
