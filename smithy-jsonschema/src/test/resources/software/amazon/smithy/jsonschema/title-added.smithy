$version: "2.0"

namespace smithy.example

@title("A structure")
structure Foo {
    bar: Integer = 0
    baz: String = ""
    bam: StringList = [],
    bat: TestEnum = "FOO"
}

@title("A list of strings")
list StringList {
    member: String
}

@title("A Test Enum!")
enum TestEnum {
    FOO
    BAR
}
