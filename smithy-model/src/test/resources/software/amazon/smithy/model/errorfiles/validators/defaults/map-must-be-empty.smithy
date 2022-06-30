$version: "2.0"

namespace smithy.example

structure Foo {
    bar: StringMap = {foo: "bar"}
}

map StringMap {
    key: String
    value: String
}
