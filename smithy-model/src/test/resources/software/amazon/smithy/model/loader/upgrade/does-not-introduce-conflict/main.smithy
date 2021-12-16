$version: "1.0"

namespace smithy.example

structure Foo {
    @default
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
