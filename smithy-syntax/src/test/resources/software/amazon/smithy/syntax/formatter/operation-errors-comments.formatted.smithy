$version: "2.0"

namespace smithy.example

operation Foo {
    errors: [
        // a
        E1
        // c
        E2
        // d
    ]
}

operation Bar {
    // a
    // b
    errors: [
        // c
    ]
}

@error("client")
structure E1 {}

@error("client")
structure E2 {}
