$version: "2.0"

namespace smithy.example

structure Foo {
    @default(0)
    alreadyDefault: Integer,

    @required
    alreadyRequired: Integer,

    boxedMember: Integer,

    previouslyBoxedTarget: Integer,

    explicitlyBoxedTarget: BoxedInteger
}

integer BoxedInteger
