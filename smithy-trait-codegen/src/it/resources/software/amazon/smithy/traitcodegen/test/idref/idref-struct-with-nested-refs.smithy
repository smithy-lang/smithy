$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.idref#IdRefStructWithNestedIds

@IdRefStructWithNestedIds(
    idRefHolder: { id: IdRefTarget1 }
    idList: [IdRefTarget1, IdRefTarget2]
    idMap: { a: IdRefTarget1, b: IdRefTarget2 }
)
structure myStruct {}

string IdRefTarget1

string IdRefTarget2
