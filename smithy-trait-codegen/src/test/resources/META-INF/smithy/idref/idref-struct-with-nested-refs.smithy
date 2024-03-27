$version: "2.0"

namespace test.smithy.traitcodegen.idref

@trait
structure IdRefStructWithNestedIds {
    @required
    idRefHolder: NestedIdRefHolder

    idList: NestedIdList

    idMap: NestedIdMap
}

@private
structure NestedIdRefHolder {
    @required
    id: IdRefNestedMember
}

@private
list NestedIdList {
    member: IdRefNestedMember
}

@private
map NestedIdMap {
    key: String
    value: IdRefNestedMember
}

@private
@idRef
string IdRefNestedMember
