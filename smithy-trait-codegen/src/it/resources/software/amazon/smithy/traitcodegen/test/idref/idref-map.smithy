$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.idref#IdRefMap

@IdRefMap(a: IdRefTarget1, b: IdRefTarget2)
structure myStruct {}

string IdRefTarget1

string IdRefTarget2
