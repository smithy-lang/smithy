$version: "2.0"

namespace test.smithy.traitcodegen

/// Checks that a deprecated annotation is added to deprecated traits along with
/// java deprecated tag
@deprecated(since: "a long long time ago", message: "because you should stop using it")
@trait
string DeprecatedStringTrait
