$version: "2.0"

metadata validators = [
    {
        name: "UnreferencedShape"
    }
]

namespace smithy.example

// A "root" shape, so does not emit.
service MyService {
    operations: [
        MyOperation
    ]
}

// Connected.
operation MyOperation {
    input := {
        foo: MyString
    }
}

// Connected.
string MyString

// Not connected.
string Baz
