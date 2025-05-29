$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.lists#NestedListTrait

@NestedListTrait([[["a"]]])
structure myStruct {}
