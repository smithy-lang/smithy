$version: "2.0"

namespace test.smithy.traitcodegen.conflicts

@trait
union UnionWithNameConflict{
    type: Type

    provider: Provider

    Id: IdMember

    union: NestedConflictUnion
}

structure IdMember {
    member: String
}

structure Type {
    member: String
}

union NestedConflictUnion {
    type: Type
    
    provider: Provider

    Id: IdMember
}
