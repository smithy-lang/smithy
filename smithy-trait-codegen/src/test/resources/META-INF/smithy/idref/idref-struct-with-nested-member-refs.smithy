$version: "2.0"

namespace test.smithy.traitcodegen.idref

@trait
structure IdRefMemberStructWithNestedIds {
    @required
    idRefHolder: NestedMemberIdRefHolder

    idList: NestedIdMemberList

    idMap: NestedIdMemberMap
}

@private
structure NestedMemberIdRefHolder {
    @required
    @idRef
    id: String
}

@private
list NestedIdMemberList {
    @idRef
    member: String
}

@private
map NestedIdMemberMap {
    key: String

    @idRef
    value: String
}
