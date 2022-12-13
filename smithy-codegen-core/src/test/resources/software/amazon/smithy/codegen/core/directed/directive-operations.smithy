$version: "1.0"

namespace smithy.example

service Foo {
    operations: [GetA, DeleteA]
}

@readonly
operation GetA {}

operation DeleteA {}
