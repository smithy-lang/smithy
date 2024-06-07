$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.maps#StringStringMap

@StringStringMap(a: "stuff", b: "other", c: "more!")
structure myStruct {}
