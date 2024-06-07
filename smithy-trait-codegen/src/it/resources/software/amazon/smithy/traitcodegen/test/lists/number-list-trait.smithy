$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.lists#NumberListTrait

@NumberListTrait([1, 2, 3, 4, 5])
structure myStruct {}
