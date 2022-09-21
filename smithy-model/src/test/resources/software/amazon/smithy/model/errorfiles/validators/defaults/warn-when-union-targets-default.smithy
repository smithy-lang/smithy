$version: "2.0"

namespace smithy.example

union Example {
    foo: PrimitiveInteger, // warn
    foo2: Integer,         // dont' warn
    bar: PrimitiveBoolean, // warn
    baz: String            // don't warn
}
