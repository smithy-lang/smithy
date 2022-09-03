// {"v1-box": false, "v1-client-zero-value": false, "v2": false}
$version: "2.0"

namespace smithy.example

structure Foo {
    // V1 models treat intEnum as normal integers, so they just see a default zero value, hence this is
    // non-nullable in v1 and v2.
    intEnumSetToZeroValueToNonNullable: MyIntEnum = 0
}

intEnum MyIntEnum {
    FOO = 0
    BAZ = 1
}
