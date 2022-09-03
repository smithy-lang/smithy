// {"v1-box": false, "v1-client-zero-value": false, "v2": false}
$version: "1.0"
namespace smithy.example

structure Foo {
    // This member is non-nullable in v1 and v2 because it targets a shape that is not marked with the box trait.
    //
    // * Upgrade: The model upgrader sees that the target is a primitive boolean, so it will add a default trait to
    //   the member and set it to false.
    // * Round-trip: When serialized and reloaded as a v2 model, the loader will see that the member has a default
    //   trait and it is set to the zero value of the target (false). The loader will not to add a synthetic box
    //   trait to the member, thereby making the nullability remain non-null in v1 implementations.
    // * v1: non-nullable
    // * v2: non-nullable
    nonNullableBooleanUnboxedTarget: MyPrimitiveBoolean
}

boolean MyPrimitiveBoolean
