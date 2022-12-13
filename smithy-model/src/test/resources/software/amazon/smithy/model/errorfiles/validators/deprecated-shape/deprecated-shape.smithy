$version: "2.0"

namespace smithy.example

@deprecated
string Foo1

@deprecated(since: "2.0")
string Foo2

@deprecated(message: "hello", since: "2.0")
string Foo3

@mixin
@deprecated
structure DeprecatedMixin {}

structure Test with [DeprecatedMixin] {
    foo1: Foo1,
    foo2: Foo2,
    foo3: Foo3
}
