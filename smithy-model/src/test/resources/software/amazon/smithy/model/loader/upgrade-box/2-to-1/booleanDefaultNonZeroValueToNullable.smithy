// {"v1-box": true, "v1-client-zero-value": true, "v2": false}
// V1 is nullable because true isn't the zero value of a boolean.
// V2 is non nullable because it has a default value.
$version: "2.0"

namespace smithy.example

structure Foo {
    booleanDefaultNonZeroValueToNullable: MyBoolean = true
}

boolean MyBoolean
