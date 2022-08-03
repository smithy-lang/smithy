$version: "2.0"

namespace smithy.api

/// Used in Smithy 1.0 to indicate that a shape is boxed.
///
/// When a boxed shape is the target of a member, the member
/// may or may not contain a value, and the member has no default value.
@trait(
    selector: """
        :test(boolean, byte, short, integer, long, float, double,
              member > :test(boolean, byte, short, integer, long, float, double))""",
    breakingChanges: [{change: "presence"}]
)
structure box {}
