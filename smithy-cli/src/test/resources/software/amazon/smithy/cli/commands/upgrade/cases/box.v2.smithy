$version: "2.0"

namespace com.example

integer BoxedInteger

@default(0)
integer NonBoxedInteger

structure StructureWithOptionalString {
    boxedTarget: BoxedInteger,

    @default(null)
    boxedMember: NonBoxedInteger,
}

union BoxyUnion {
    boxedTarget: BoxedInteger,

    boxedMember: NonBoxedInteger,
}

list BadSparseList {
    member: NonBoxedInteger,
}

@uniqueItems
list BadSparseSet {
    member: NonBoxedInteger,
}

map BadSparseMap {
    key: String,

    value: NonBoxedInteger,
}
