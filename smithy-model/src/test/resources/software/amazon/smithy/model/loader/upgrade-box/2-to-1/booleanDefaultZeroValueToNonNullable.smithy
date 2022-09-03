// {"v1-box": true, "v1-client-zero-value": false, "v2": false}
// V1 style box checks will think this member is nullable because it
// targets a shape with the box trait.
$version: "2.0"

namespace smithy.example

structure Foo {
    booleanDefaultZeroValueToNonNullable: MyBoolean = false
}

boolean MyBoolean
