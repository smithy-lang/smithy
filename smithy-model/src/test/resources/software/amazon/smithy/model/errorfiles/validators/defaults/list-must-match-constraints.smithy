$version: "2.0"

namespace smithy.example

structure Foo {
    bar: StringList = []
}

@length(min: 1)
list StringList {
    member: String
}
