$version: "2.0"

namespace smithy.example

structure Foo {
    bar: Integer = 0
    baz: String = ""
    bam: StringList = [],
    bat: TestEnum = "FOO"
}

list StringList {
    member: String
}

enum TestEnum {
    FOO
    BAR
}
