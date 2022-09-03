// {"v1-box": true, "v1-client-zero-value": true, "v2": true}
$version: "1.0"
namespace smithy.example

structure Foo {
    // This member is nullable in v1 and v2.
    //
    // * Upgrade: The model upgrader sees that the target shape is marked with the box trait in v1, so it does not add
    //   a default trait to the member. Note that the prelude shapes that were boxed in v1 continue to carry a box
    //   trait.
    // * Round-trip: When serialized and reloaded as a v2 model, the loader sees that the target shape is marked
    //   with the box trait and that the shape has no default trait, so it applies the box trait.
    // * v1: nullable
    // * v2: nullable
    nullableBooleanBoxedTarget: Boolean,
}
