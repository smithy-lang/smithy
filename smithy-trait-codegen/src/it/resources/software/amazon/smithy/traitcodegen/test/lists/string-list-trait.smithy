$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.lists#StringListTrait

@StringListTrait(["a", "b", "c", "d"])
structure myStruct {}
