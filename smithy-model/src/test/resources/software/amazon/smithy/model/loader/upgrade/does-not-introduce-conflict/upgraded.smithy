$version: "2.0"

namespace smithy.example

structure Foo {
    @default
    alreadyDefault: Integer,

    @required
    alreadyRequired: Integer,

    boxedMember: Integer,

    previouslyBoxedTarget: Integer,

    explicitlyBoxedTarget: BoxedInteger
}

integer BoxedInteger
