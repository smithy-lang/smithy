$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.enums#MyIntEnumTrait
use test.smithy.traitcodegen.enums#MyEnumTrait

@MyIntEnumTrait(1)
@MyEnumTrait("1")
structure myStruct {}
