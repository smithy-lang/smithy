$version: "2.0"

namespace smithy.example

/// A
string A

/// B1
/// B2
structure B {}

// Regular comment
/// C
@sensitive
integer C
// Trailing comments are not omitted.
