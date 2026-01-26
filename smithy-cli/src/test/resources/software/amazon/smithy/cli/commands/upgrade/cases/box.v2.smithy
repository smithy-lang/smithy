$version: "2.0"

namespace com.example

integer BoxedInteger

@default(0)
integer NonBoxedInteger

boolean BoxedBoolean

structure StructureWithOptionalString {
    boxedTarget: BoxedInteger,

    @default(null)
    boxedMember: NonBoxedInteger,
}

union BoxyUnion {
    boxedTarget: BoxedInteger,

    boxedMember: NonBoxedInteger,
}

@sparse
list BadSparseList {
    member: NonBoxedInteger,
}

@uniqueItems
list BadSparseSet {
    member: NonBoxedInteger,
}

@sparse
map BadSparseMap {
    key: String,

    value: NonBoxedInteger,
}

@sparse
list GoodSparseList {

    member: BoxedBoolean
}
