$version: "2.0"

namespace smithy.example

structure Foo {
    bar: Bar = "hi"
    baz: Baz = "bye"
}

@length(min: 5)
string Bar

@pattern("^[A-Z]+$")
string Baz
