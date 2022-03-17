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
