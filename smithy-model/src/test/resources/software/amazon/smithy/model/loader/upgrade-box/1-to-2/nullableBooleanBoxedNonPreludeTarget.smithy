// {"v1-box": true, "v1-client-zero-value": true, "v2": true}
$version: "1.0"
namespace smithy.example

structure Foo {
    // This is the same test as nullableBooleanBoxedTarget, but uses a non-prelude shape to ensure that the v2->v1
    // box trait patching doesn't rely on the implicit box trait that is present on some prelude shapes.
    //
    // * Upgrade: The model upgrader sees that the target shape is marked with the box trait, so it does not add a
    //   default trait to the member.
    // * Round-trip: When serialized and reloaded as a v2 model, the loader sees that the member has no default trait,
    //   so it applies the box trait, thereby making v1 and v2 implementations both know the member is nullable.
    //   Note that box traits are synthetic, so the box trait applied to MyBoolean is lost when serializing the model
    //   as a v2 model. This means that for the loader to patch in v1 box traits, it can't rely in the box trait.
    // * v1: nullable
    // * v2: nullable
    nullableBooleanBoxedNonPreludeTarget: MyBoolean
}

@box
boolean MyBoolean
