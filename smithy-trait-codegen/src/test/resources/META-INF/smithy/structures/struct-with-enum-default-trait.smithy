$version: "2.0"

namespace test.smithy.traitcodegen.structures

@trait
structure StructWithEnumDefault {
    memberA: EnumA = "1"
    memberB: EnumB = 3
}

enum EnumA {
    ONE = "1"
    TWO = "2"
}

intEnum EnumB {
    THREE = 3
    FOUR = 4
}
