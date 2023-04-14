// Syntax error at line 8, column 5: Duplicate operation input property for `com.foo#GetFoo`
$version: "2.0"

namespace com.foo

operation GetFoo {
    input: GetFooInput,
    input: GetFooInput,
}

@input
structure GetFooInput {}
