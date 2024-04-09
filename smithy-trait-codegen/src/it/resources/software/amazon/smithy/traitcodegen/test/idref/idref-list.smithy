$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.idref#IdRefList

@IdRefList([IdRefTarget1, IdRefTarget2])
structure myStruct {}

string IdRefTarget1

string IdRefTarget2
