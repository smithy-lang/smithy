$version: "1.0"

namespace com.example

@box
integer BoxedInteger

integer NonBoxedInteger

structure StructureWithOptionalString {
    boxedTarget: BoxedInteger,

    @box
    boxedMember: NonBoxedInteger,
}

union BoxyUnion {
    boxedTarget: BoxedInteger,

    @box
    boxedMember: NonBoxedInteger,
}

list BadSparseList {
    @box
    member: NonBoxedInteger,
}

set BadSparseSet {
    @box
    member: NonBoxedInteger,
}

map BadSparseMap {
    key: String,

    @box
    value: NonBoxedInteger,
}
