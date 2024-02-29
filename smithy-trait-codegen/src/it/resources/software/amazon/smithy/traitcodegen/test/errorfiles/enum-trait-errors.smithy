$version: "2.0"

namespace test.smithy.traitcodegen.enums

@StringEnum("bad")
string notAValidVariant

@StringEnum("YES")
string incorrectValueCasing
