$version: "2.0"

namespace smithy.example

structure Foo {
    @default(["foo"])
    bar: StringList
}

list StringList {
    member: String
}
