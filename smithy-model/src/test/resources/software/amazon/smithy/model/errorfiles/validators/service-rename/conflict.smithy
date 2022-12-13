$version: "2.0"

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
    output: SayHelloOutput
}

@input
structure SayHelloInput {
    foo: Foo,
    baz: Baz,
}

@output
structure SayHelloOutput {}

structure Foo {}

structure Baz {}
