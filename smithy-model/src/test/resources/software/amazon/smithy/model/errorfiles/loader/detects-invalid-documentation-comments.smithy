/// Invalid 1 (before control)
$version: "2.0"

/// Invalid 2 (before namespace)
namespace smithy.example

/// Invalid 3 (before use)
use smithy.api#Boolean

@enum([
    /// Invalid 4 (inside trait)
    { name: "X", value: "X"}
])
/// Invalid 5 (not before traits)
string Features /// Valid docs for Foo

@sensitive
/// Invalid 6 (not before traits)
map Foo
/// Invalid 7 (inside shape line)
{
    key: Features,
    value: Boolean
    /// Invalid 8 (dangling)
}

/// Invalid 9 (dangling)
