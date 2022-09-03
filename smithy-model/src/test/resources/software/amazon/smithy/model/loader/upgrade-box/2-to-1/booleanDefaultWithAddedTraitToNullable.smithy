// {"v1-box": true, "v1-client-zero-value": true, "v2": false}
$version: "2.0"

namespace smithy.example

structure Foo {
    // The addedDefault trait tells v1 implementations to ignore the default trait on the member.
    @addedDefault
    booleanDefaultWithAddedTraitToNullable: MyBoolean = false
}

boolean MyBoolean
