$version: "1.0"
namespace smithy.api

/// Used in Smithy 1.0 to indicate that a shape is boxed.
/// This trait has no effect in Smithy IDL 2.0.
///
/// When a boxed shape is the target of a member, the member
/// may or may not contain a value, and the member has no default value.
@trait(
    selector: """
        :test(boolean, byte, short, integer, long, float, double,
              member > :test(boolean, byte, short, integer, long, float, double))"""
)
structure box {}

// The box trait was removed in IDL 2.0, so it can't appear on IDL 2.0 prelude shapes
// Apply the box trait in the 1.0 prelude so that previous code written to check for
// the box trait on these shapes continues to function.

apply Boolean @box

apply Byte @box

apply Short @box

apply Integer @box

apply Long @box

apply Float @box

apply Double @box
