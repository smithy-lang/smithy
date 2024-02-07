$version: "2.0"

namespace test.smithy.traitcodegen

@ResponseType("bad")
string notAValidVariant

@ResponseType("YES")
string incorrectValueCasing
