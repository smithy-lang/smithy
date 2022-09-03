$version: "2.0"

namespace smithy.example

structure Foo {
    @default(0)
    alreadyDefault: PrimitiveInteger,

    @required
    @default(0)
    alreadyRequired: PrimitiveInteger,

    boxedMember: PrimitiveInteger,

    previouslyBoxedTarget: Integer,

    explicitlyBoxedTarget: BoxedInteger,

    @required
    @default(0)
    customPrimitiveLong: MyPrimitiveLong
}

integer BoxedInteger

long MyPrimitiveLong
