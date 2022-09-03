// {"v1": false, "v2": false}
$version: "2.0"

namespace smithy.example

structure Foo {
    // This member targets the prelude smithy.ap#Boolean shape which is
    // considered nullable in v1 but non-nullable in v2.
    booleanDefaultZeroValueToNonNullablePrelude: Boolean = false
}
