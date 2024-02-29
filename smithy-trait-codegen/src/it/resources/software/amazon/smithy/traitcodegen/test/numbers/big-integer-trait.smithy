$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.numbers#BigIntegerTrait

@BigIntegerTrait(100)
structure myStruct {}
