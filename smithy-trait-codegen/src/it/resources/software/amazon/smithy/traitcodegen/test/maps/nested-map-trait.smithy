$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.maps#NestedMapTrait

@NestedMapTrait(a: {b: {c: "d"}})
structure myStruct {}
