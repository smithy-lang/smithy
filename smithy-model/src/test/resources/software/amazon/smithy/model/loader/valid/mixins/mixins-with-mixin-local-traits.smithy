$version: "2.0"

namespace smithy.example

/// This mixin is used to make things have a foo member,
/// but it can't be used outside of this namespace.
@private
@sensitive
@mixin(localTraits: [private, documentation])
structure PrivateMixin {
    foo: String
}

structure PublicShape with [PrivateMixin] {}
