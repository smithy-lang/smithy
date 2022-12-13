$version: "2.0"

namespace smithy.example

structure Foo {
    bar: Baz = 1
    baz: Baz = 0
}

@range(min: 1, max: 10)
integer Baz
