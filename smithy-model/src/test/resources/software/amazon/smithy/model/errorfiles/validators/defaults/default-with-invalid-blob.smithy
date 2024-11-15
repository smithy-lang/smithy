$version: "2.0"

namespace smithy.example

structure Foo {
    invalidBlob: Blob = "{}"
}

@default("{}") // invalid default value
blob InvalidBlob
