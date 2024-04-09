$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.numbers#DoubleTrait

@DoubleTrait(100.01)
structure myStruct {}
