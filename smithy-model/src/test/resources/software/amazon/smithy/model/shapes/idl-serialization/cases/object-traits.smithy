$version: "2.0"

namespace ns.foo

@trait
map MapTrait {
    key: String
    value: String
}

@trait
structure StructureTrait {
    collectionMember: StringList
    nestedMember: NestedMember
    shapeIdMember: ShapeId
    staticMember: String
}

@trait
union UnionTrait {
    boolean: Boolean
    string: String
}

structure NestedMember {
    shapeIds: ShapeIdList
}

list ShapeIdList {
    member: ShapeId
}

list StringList {
    member: String
}

@MapTrait
@StructureTrait
string EmptyBody

@MapTrait(
    bar: "baz"
    foo: "bar"
    "must be quoted": "bam1"
    "must.be.quoted": "bam2"
    "must.be#quoted": "bam3"
    "must.be#quoted$too": "bam4"
)
@StructureTrait(
    collectionMember: [
        "foo"
        "bar"
    ]
    nestedMember: {
        shapeIds: [
            String
            EmptyBody
        ]
    }
    shapeIdMember: UnionTrait
    staticMember: "Foo"
)
@UnionTrait(
    boolean: false
)
string NonEmptyBody

@idRef
string ShapeId
