$version: "2.0"

namespace smithy.example

operation Foo {
    input := @metadata(key: "bar") {
    }
    output := @metadata(key: "baz") {
    }
}
