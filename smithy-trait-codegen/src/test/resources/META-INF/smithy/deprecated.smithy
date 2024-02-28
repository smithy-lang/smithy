$version: "2.0"

namespace test.smithy.traitcodegen

// ==================
//  Deprecation tests
// ==================
/// Checks that a deprecated annotation is added to deprecated traits
@deprecated(since: "a long long time ago", message: "because you should stop using it")
@trait
string DeprecatedStringTrait
