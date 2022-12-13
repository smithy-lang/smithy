$version: "2.0"

namespace smithy.example

structure Foo {
    bar: StringMap = {}
}

@length(min: 1)
map StringMap {
    key: String
    value: String
}
