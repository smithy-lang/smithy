$version: "2.0"

namespace test.smithy.traitcodegen.structures

enum Foo {
    BAR
}

@idRef
string IdRefPointer

@trait
structure StructWithIdrefMemberWithDefault {
    @idRef
    idRefDefault: String = Foo$BAR

    idRefTargetDefault: IdRefPointer = Foo$BAR
}
