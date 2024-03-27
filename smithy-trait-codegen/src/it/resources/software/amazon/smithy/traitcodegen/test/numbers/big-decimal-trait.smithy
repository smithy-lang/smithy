$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.numbers#BigDecimalTrait

@BigDecimalTrait(100.01)
structure myStruct {}
