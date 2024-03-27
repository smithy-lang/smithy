$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.idref#IdRefStruct

@IdRefStruct(fieldA: IdRefTarget1)
structure myStruct {}

string IdRefTarget1
