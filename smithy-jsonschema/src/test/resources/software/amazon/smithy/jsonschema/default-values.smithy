$version: "2.0"

namespace smithy.example

structure Foo {
    bar: Integer = 0
    baz: String = ""
    bam: StringList = []
}

list StringList {
    member: String
}
