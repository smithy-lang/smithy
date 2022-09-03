// {"v1-box": true, "v1-client-zero-value": true, "v2": true}
$version: "1.0"
namespace smithy.example

structure Foo {
    // This member is nullable in both v1 and v2 models because of the box trait on the member.
    //
    // * Upgrade: The model upgrader will see the box trait and not add a default trait.
    // * Round-trip: When serialized and reloaded as a v2 model, the box traits will be gone since
    //   they're synthetic, but the loader will patch in a synthetic box trait on the member because it has no
    //   default trait.
    // * v1: nullable
    // * v2: nullable
    @box
    nullableBooleanBoxedMember: MyPrimitiveBoolean
}

boolean MyPrimitiveBoolean
