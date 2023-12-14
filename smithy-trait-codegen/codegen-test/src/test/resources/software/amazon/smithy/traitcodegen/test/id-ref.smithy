$version: "2.0"

namespace test.smithy.traitcodegen

@IdRefString(IdRefTarget1)
@IdRefList([IdRefTarget1, IdRefTarget2])
@IdRefMap(
    a: IdRefTarget1
    b: IdRefTarget2
)
@IdRefStruct(fieldA: IdRefTarget1)
@IdRefStructWithNestedIds(
    idRefHolder: {
        id: IdRefTarget1
    }
    idList: [IdRefTarget1, IdRefTarget2]
    idMap: {
        a: IdRefTarget1
        b: IdRefTarget2
    }
)
structure myStruct {}

string IdRefTarget1

string IdRefTarget2
