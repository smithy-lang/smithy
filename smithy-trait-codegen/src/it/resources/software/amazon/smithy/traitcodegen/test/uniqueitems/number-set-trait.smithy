$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.uniqueitems#NumberSetTrait

@NumberSetTrait([1, 2, 3, 4])
structure myStruct {}
