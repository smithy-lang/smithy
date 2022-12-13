// {"v1-box": true, "v1-client-zero-value": true, "v2": false}
// Required in v2 makes it non-nullable.
// It has no default so it's nullable in all v1 checks.
$version: "2.0"

namespace smithy.example

structure Foo {
    @required
    booleanRequiredToNullable: Boolean
}
