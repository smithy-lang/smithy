$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "Test",
        severity: "WARNING",
        configuration: {
            selector: ":not([id='smithy.example#NoMatches'])"
        }
    }
]

metadata suppressions = []

namespace smithy.example

structure Foo {
    foo: String
}

structure Baz {
    baz: String
}
