// {"v1": true, "v2": false}
// Required in v2 makes it non-nullable.
// It has no default so it's nullable in v1.
$version: "2.0"

namespace smithy.example

structure Foo {
    @required
    booleanRequiredToNullable: Boolean
}
