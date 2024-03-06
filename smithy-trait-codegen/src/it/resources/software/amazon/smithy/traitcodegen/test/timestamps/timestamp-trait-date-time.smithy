$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.timestamps#TimestampTrait

@TimestampTrait("1985-04-12T23:20:50.52Z")
structure myStruct {}
