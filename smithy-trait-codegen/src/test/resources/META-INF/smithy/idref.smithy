$version: "2.0"

namespace test.smithy.traitcodegen

// ==================
//  IdRef tests
// ==================
// The following traits check to make sure that Strings are converted to ShapeIds
// when an @IdRef trait is added to a string

@trait
@idRef
string IdRefString

@trait
list IdRefList {
    member: IdRefmember
}

@trait
map IdRefMap {
    key: String
    value: IdRefmember
}

@trait
structure IdRefStruct {
    fieldA: IdRefmember
}

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
    id: IdRefmember
}

@private
list NestedIdList {
    member: IdRefmember
}

@private
map NestedIdMap {
    key: String
    value: IdRefmember
}

@private
@idRef
string IdRefmember
