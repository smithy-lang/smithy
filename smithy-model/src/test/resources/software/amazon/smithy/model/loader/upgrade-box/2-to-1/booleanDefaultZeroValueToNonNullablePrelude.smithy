// {"v1-box": true, "v1-client-zero-value": false, "v2": false}
// v1-box: Returns true, incorrectly, because v1 prelude shapes have the box traits. Implementations need
//     to be updated to use NullableIndex with CLIENT_ZERO_VALUE_V1 to work both backward compatibly with
//     v1 and to no turn every v2 member that targets a boxed prelude shape to nullable.
// v1-client-zero-value: Does not look at box traits and instead just looks at zero values on members.
//     This check works correctly.
// v2: false. Like v1-client-zero-value, does not look at box traits.
$version: "2.0"

namespace smithy.example

structure Foo {
    // This member targets the prelude smithy.ap#Boolean shape which is
    // considered nullable in v1 but non-nullable in v2.
    booleanDefaultZeroValueToNonNullablePrelude: Boolean = false
}
