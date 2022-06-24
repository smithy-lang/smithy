$version: "2.0"

namespace smithy.example

@uniqueItems
list Floats {
    member: Float
}

@uniqueItems
list Doubles {
    member: Double
}

@uniqueItems
list Documents {
    member: Document
}

structure NestedLists {
    floats: Floats,
    doubles: Doubles,
    documents: Documents
}

@uniqueItems
list Structures {
    member: NestedLists
}
