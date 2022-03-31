$version: "2.0"

namespace com.example

integer BoxedInteger

integer NonBoxedInteger

structure StructureWithOptionalString {
    boxedTarget: BoxedInteger,

    boxedMember: NonBoxedInteger,
}

union BoxyUnion {
    boxedTarget: BoxedInteger,

    boxedMember: NonBoxedInteger,
}

list BadSparseList {
    member: NonBoxedInteger,
}

set BadSparseSet {
    member: NonBoxedInteger,
}

map BadSparseMap {
    key: String,

    value: NonBoxedInteger,
}
