$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.idref#IdRefString

@IdRefString(IdRefTarget1)
structure myStruct {}

string IdRefTarget1
