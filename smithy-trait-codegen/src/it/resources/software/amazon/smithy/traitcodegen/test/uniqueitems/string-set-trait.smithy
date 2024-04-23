$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.uniqueitems#StringSetTrait

@StringSetTrait(["a", "b", "c", "d"])
structure myStruct {}
