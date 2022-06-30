$version: "2.0"

namespace smithy.example

structure Foo {
    bar: Bar = "FOO" // valid
    baz: Bar = "baz" // invalid
}

enum Bar {
    FOO
    BAZ = "BAZ"
}
