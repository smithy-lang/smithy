$version: "2.0"

namespace smithy.example

@http(method: GET, uri: "/") // <- Use "GET" not GET!
operation Foo {
    input: FooInput,
    output: FooOutput
}

@input
structure FooInput {}

@output
structure FooOutput {}
