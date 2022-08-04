// Parse error at line 8, column 5 near `input`: Found 'i', but expected one of the following tokens: 'o' 'e' '}' | Model
$version: "2.0"

namespace com.foo

operation GetFoo {
    input: GetFooInput,
    input: GetFooInput,
}

@input
structure GetFooInput {}
