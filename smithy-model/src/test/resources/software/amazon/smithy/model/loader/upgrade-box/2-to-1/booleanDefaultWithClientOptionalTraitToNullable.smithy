// {"v1-box": true, "v1-client-zero-value": true, "v2": true}
$version: "2.0"

namespace smithy.example

structure Foo {
    // The clientOptional trait tells v1 and v2 (client mode) implementations to ignore the default trait on the
    // member.
    @clientOptional
    booleanDefaultWithClientOptionalTraitToNullable: MyBoolean = false
}

boolean MyBoolean
