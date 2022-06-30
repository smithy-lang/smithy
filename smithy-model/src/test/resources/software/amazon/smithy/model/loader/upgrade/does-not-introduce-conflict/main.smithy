$version: "1.0"

namespace smithy.example

structure Foo {
    @default(0)
    alreadyDefault: PrimitiveInteger,

    @required
    alreadyRequired: PrimitiveInteger,

    @box
    boxedMember: PrimitiveInteger,

    previouslyBoxedTarget: Integer,

    explicitlyBoxedTarget: BoxedInteger
}

@box
integer BoxedInteger
