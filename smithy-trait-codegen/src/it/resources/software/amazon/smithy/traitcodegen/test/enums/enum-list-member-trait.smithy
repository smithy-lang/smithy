$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.enums#EnumListMemberTrait

@EnumListMemberTrait(value: ["some", "none", "some"])
structure myStruct {}
