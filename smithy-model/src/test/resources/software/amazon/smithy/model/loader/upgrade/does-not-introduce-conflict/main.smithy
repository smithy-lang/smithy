$version: "1.0"

namespace smithy.example

structure Foo {
    alreadyDefault: PrimitiveInteger,

    @required
    alreadyRequired: PrimitiveInteger,

    @box
    boxedMember: PrimitiveInteger,

    // smithy.api#Integer is not boxed in 2.0 but was in 1.0 so this member is
    // considered implicitly boxed.
    previouslyBoxedTarget: Integer,

    explicitlyBoxedTarget: BoxedInteger,

    // This shape cannot be boxed because it targeted a long that was a primitive.
    // This shape also cannot be default because it's required.
    @required
    customPrimitiveLong: MyPrimitiveLong
}

@box
integer BoxedInteger

long MyPrimitiveLong
