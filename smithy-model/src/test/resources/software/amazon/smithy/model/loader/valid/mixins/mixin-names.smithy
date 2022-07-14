$version: "2.0"

namespace smithy.example

@mixin
integer integerPrefixed

@mixin
integer NotPrefixed

// This is related to a bug that was present where only
// the first character of the identifier following a mixin
// was ever looked at. This ensures that happens to start
// with an i and be the same length as "integer" doesn't
// trip up the parser.
@mixin
integer iiiiiii

integer MixedInt with [NotPrefixed integerPrefixed iiiiiii]

integer SafeInt
