$version: "2.0"

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
    output: SayHelloOutput
}

@input
structure SayHelloInput {
    foo: Foo,
    baz: Baz
}

@output
structure SayHelloOutput {}

structure Foo {}

structure Baz {}
