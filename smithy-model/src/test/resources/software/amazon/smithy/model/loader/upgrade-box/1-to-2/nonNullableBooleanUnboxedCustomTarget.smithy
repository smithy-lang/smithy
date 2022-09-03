// {"v1-box": false, "v1-client-zero-value": false, "v2": false}
$version: "1.0"
namespace smithy.example

structure Foo {
    // This member is considered non-nullable in v1 and v2 because it targets a primitive boolean. It is also
    // considered non-nullable in v2 because it is marked as required.
    //
    // * Upgrade: The model upgrader will add a default trait to the member and set the member to the zero
    //   value of the target (false in this case).
    // * Round-trip: When serialized and reloaded as a v2 model, the loader will see the default trait, see
    //   that it is the zero value for the type so it will not add a synthetic box trait to the member.
    // * v1: non-nullable
    // * v2: non-nullable
    @required
    nonNullableBooleanUnboxedCustomTarget: MyPrimitiveBoolean
}

boolean MyPrimitiveBoolean
