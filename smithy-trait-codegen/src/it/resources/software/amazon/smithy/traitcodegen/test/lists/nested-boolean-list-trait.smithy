$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.lists#NestedBooleanListTrait

@NestedBooleanListTrait([[[true]]])
structure myStruct {}
