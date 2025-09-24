$version: "2.0"

namespace test.smithy.traitcodegen.conflicts

@trait
structure StructWithNameConflict {
    provider: Provider

    list: ListOfList

    map: MapOfMap

    set: SetOfSet

    shapeId: ShapeId
}


enum Provider {
    ONE = "1"
}

structure ShapeId {
    @idRef
    id: String
}

structure List {
    provider: Provider
}

structure Map {
    member: String
}

structure Set {
    member: String
}

list ListOfList {
    member: List
}

@uniqueItems
list SetOfSet{
    member: Set
}

map MapOfMap {
    key: String
    value: Map
}
