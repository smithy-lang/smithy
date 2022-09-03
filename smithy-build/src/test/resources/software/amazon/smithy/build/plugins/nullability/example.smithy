$version: "1.0"

namespace smithy.example

structure Foo {
    nullableIntegerBoxedTarget: Integer,
    nullableIntegerBoxedNonPreludeTarget: MyInteger,
    @required
    nullableIntegerInV1BoxedTargetRequired: Integer,
    nonNullableIntegerUnboxedTarget: MyPrimitiveInteger,
    @box
    nullableIntegerBoxedMember: MyPrimitiveInteger,
    @required
    nonNullableIntegerUnboxedCustomTarget: MyPrimitiveInteger,
}

@box
integer MyInteger

integer MyPrimitiveInteger
