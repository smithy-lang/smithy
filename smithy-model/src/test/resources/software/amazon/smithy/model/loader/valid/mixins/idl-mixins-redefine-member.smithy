$version: "2.0"

namespace smithy.example

@mixin
structure MixinStructure {
    @internal
    redefineable: String
}

structure MixedStructure with [MixinStructure] {
    // Since the target hasn't changed, this is an acceptable redefinition.
    // Traits are still inherited as normal, they just don't have to use
    // apply to introduce new ones.
    @required
    redefineable: String
}
