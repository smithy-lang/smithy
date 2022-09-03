// {"v1-box": true, "v1-client-zero-value": true, "v2": false}
$version: "1.0"
namespace smithy.example

structure Foo {
    // This member is nullable in v1, but should be considered non-nullable in v2.
    //
    // * Upgrade: The model upgrader sees that the target shape is marked with the box trait, so it does not add a
    //   default trait to the member. Note that the prelude shapes that were boxed in v1 continue to carry a box
    //   trait. They're special-cased in the loader so that no deprecation warnings are emitted.
    // * Round-trip: When serialized and reloaded as a v2 model, the loader sees that the target shape is marked
    //   with the box trait and that the shape has no default trait, so it applies the box trait.
    // * v1: nullable
    // * v2: non-nullable because of the required trait
    @required
    nullableBooleanInV1BoxedTargetRequired: Boolean
}
