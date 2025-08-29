$version: "2.0"

namespace test.smithy.traitcodegen.enums

@mixin
@trait
enum MyMixinEnum {
    THREE = "3"
}

@mixin
@trait
intEnum MyMixinIntEnum {
    THREE = 3
}

@trait
enum MyEnumTrait with [MyMixinEnum] {
    ONE = "1"
    TWO = "2"
}

@trait
intEnum MyIntEnumTrait with [MyMixinIntEnum]{
    ONE = 1
    TWO = 2
}