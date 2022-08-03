$version: "2.0"

namespace smithy.example

structure Foo {
    @default(0)
    alreadyDefault: PrimitiveInteger,

    @required
    alreadyRequired: PrimitiveInteger,

    boxedMember: PrimitiveInteger,

    previouslyBoxedTarget: Integer,

    explicitlyBoxedTarget: BoxedInteger,

    @required
    customPrimitiveLong: MyPrimitiveLong
}

integer BoxedInteger

long MyPrimitiveLong
